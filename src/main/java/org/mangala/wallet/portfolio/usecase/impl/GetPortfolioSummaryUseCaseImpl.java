package org.mangala.wallet.portfolio.usecase.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.portfolio.adapter.repository.UserPortfolioBalanceRepository;
import org.mangala.wallet.portfolio.adapter.web.dto.PortfolioSummaryResponse;
import org.mangala.wallet.portfolio.adapter.web.dto.TokenHolding;
import org.mangala.wallet.portfolio.domain.UserPortfolioBalanceEntity;
import org.mangala.wallet.portfolio.usecase.GetPortfolioSummaryUseCase;
import org.mangala.wallet.price.PriceService;
import org.mangala.wallet.price.service.PriceHistoryProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetPortfolioSummaryUseCaseImpl implements GetPortfolioSummaryUseCase {

    private final UserPortfolioBalanceRepository portfolioRepository;
    private final PriceService priceService;
    private final PriceHistoryProvider priceHistoryService;

    @Override
    @Cacheable(value = "portfolioSummary", key = "#userId", unless = "#result == null")
    public PortfolioSummaryResponse getPortfolioSummary(UUID userId) {
        log.debug("Fetching portfolio summary for user: {}", userId);

        List<UserPortfolioBalanceEntity> balances = portfolioRepository.findNonZeroBalancesByUserId(userId);

        return buildPortfolioResponse(balances, userId);
    }

    @Override
    @Cacheable(value = "portfolioSummaryByChain", key = "#userId + ':' + #chainType", unless = "#result == null")
    public PortfolioSummaryResponse getPortfolioSummaryByChain(UUID userId, String chainType) {
        log.debug("Fetching portfolio summary for user: {} chain: {}", userId, chainType);

        List<UserPortfolioBalanceEntity> balances = portfolioRepository
                .findByUserIdAndChainType(userId, chainType.toUpperCase())
                .stream()
                .filter(b -> b.getTotalBalance().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        return buildPortfolioResponse(balances, userId);
    }

    private PortfolioSummaryResponse buildPortfolioResponse(List<UserPortfolioBalanceEntity> balances, UUID userId) {
        if (balances.isEmpty()) {
            return PortfolioSummaryResponse.builder()
                    .totalValueUsd(BigDecimal.ZERO)
                    .change24h(PortfolioSummaryResponse.Change24h.builder()
                            .amountUsd(BigDecimal.ZERO)
                            .percentage(BigDecimal.ZERO)
                            .build())
                    .tokens(Collections.emptyList())
                    .chainSummary(Collections.emptyMap())
                    .lastUpdatedAt(Instant.now())
                    .build();
        }

        // Calculate token holdings with prices
        List<TokenHolding> tokenHoldings = new ArrayList<>();
        Map<String, BigDecimal> chainSummary = new HashMap<>();
        BigDecimal totalValueUsd = BigDecimal.ZERO;
        BigDecimal totalChange24hValue = BigDecimal.ZERO;
        LocalDateTime latestUpdate = null;

        for (UserPortfolioBalanceEntity balance : balances) {
            BigDecimal price = priceService.getPrice(balance.getSymbol(), balance.getChainType());
            BigDecimal change24h = priceHistoryService.get24hChangePercent(balance.getSymbol(), balance.getChainType());

            BigDecimal tokenValue = balance.getTotalBalance().multiply(price);
            totalValueUsd = totalValueUsd.add(tokenValue);

            // Calculate 24h change contribution
            if (change24h.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal previousValue = tokenValue.divide(
                        BigDecimal.ONE.add(change24h.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)),
                        8, RoundingMode.HALF_UP);
                totalChange24hValue = totalChange24hValue.add(tokenValue.subtract(previousValue));
            }

            // Update chain summary
            chainSummary.merge(balance.getChainType(), tokenValue, BigDecimal::add);

            // Track latest update
            if (latestUpdate == null || balance.getLastUpdatedAt().isAfter(latestUpdate)) {
                latestUpdate = balance.getLastUpdatedAt();
            }

            tokenHoldings.add(TokenHolding.builder()
                    .symbol(balance.getSymbol())
                    .name(balance.getName())
                    .chain(balance.getChainType())
                    .contractAddress(balance.getContractAddress())
                    .quantity(balance.getTotalBalance())
                    .priceUsd(price)
                    .valueUsd(tokenValue)
                    .change24hPercent(change24h)
                    .walletCount(balance.getWalletCount())
                    .build());
        }

        // Calculate allocation percentages
        final BigDecimal finalTotalValue = totalValueUsd;
        if (finalTotalValue.compareTo(BigDecimal.ZERO) > 0) {
            tokenHoldings = tokenHoldings.stream()
                    .map(holding -> TokenHolding.builder()
                            .symbol(holding.getSymbol())
                            .name(holding.getName())
                            .chain(holding.getChain())
                            .contractAddress(holding.getContractAddress())
                            .quantity(holding.getQuantity())
                            .priceUsd(holding.getPriceUsd())
                            .valueUsd(holding.getValueUsd())
                            .change24hPercent(holding.getChange24hPercent())
                            .walletCount(holding.getWalletCount())
                            .allocationPercent(holding.getValueUsd()
                                    .divide(finalTotalValue, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP))
                            .build())
                    .sorted(Comparator.comparing(TokenHolding::getValueUsd).reversed())
                    .collect(Collectors.toList());
        }

        // Calculate overall 24h change percentage
        BigDecimal previousTotalValue = totalValueUsd.subtract(totalChange24hValue);
        BigDecimal change24hPercent = BigDecimal.ZERO;
        if (previousTotalValue.compareTo(BigDecimal.ZERO) > 0) {
            change24hPercent = totalChange24hValue
                    .divide(previousTotalValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return PortfolioSummaryResponse.builder()
                .totalValueUsd(totalValueUsd.setScale(2, RoundingMode.HALF_UP))
                .change24h(PortfolioSummaryResponse.Change24h.builder()
                        .amountUsd(totalChange24hValue.setScale(2, RoundingMode.HALF_UP))
                        .percentage(change24hPercent)
                        .build())
                .tokens(tokenHoldings)
                .chainSummary(chainSummary.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().setScale(2, RoundingMode.HALF_UP))))
                .lastUpdatedAt(latestUpdate != null
                        ? latestUpdate.atZone(java.time.ZoneId.systemDefault()).toInstant()
                        : Instant.now())
                .build();
    }
}
