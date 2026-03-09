package org.mangala.wallet.price.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.price.PriceService;
import org.mangala.wallet.price.adapter.repository.TokenPriceHistoryRepository;
import org.mangala.wallet.price.domain.TokenPriceHistoryEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing token price history and calculating 24h changes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceHistoryService implements PriceHistoryProvider {

    private final TokenPriceHistoryRepository priceHistoryRepository;
    private final PriceService priceService;

    private static final List<String[]> TRACKED_TOKENS = List.of(
            new String[]{"ETH", "ETHEREUM"},
            new String[]{"BNB", "BSC"},
            new String[]{"MATIC", "POLYGON"},
            new String[]{"SOL", "SOLANA"},
            new String[]{"USDT", "ETHEREUM"},
            new String[]{"USDC", "ETHEREUM"},
            new String[]{"DAI", "ETHEREUM"},
            new String[]{"WBTC", "ETHEREUM"},
            new String[]{"LINK", "ETHEREUM"},
            new String[]{"UNI", "ETHEREUM"},
            new String[]{"AAVE", "ETHEREUM"}
    );

    /**
     * Record current prices for all tracked tokens.
     */
    @Override
    @Transactional
    public void recordCurrentPrices() {
        LocalDateTime now = LocalDateTime.now();
        int recordedCount = 0;

        for (String[] token : TRACKED_TOKENS) {
            String symbol = token[0];
            String chainType = token[1];

            try {
                BigDecimal price = priceService.getPrice(symbol, chainType);

                if (price.compareTo(BigDecimal.ZERO) > 0) {
                    TokenPriceHistoryEntity entity = TokenPriceHistoryEntity.builder()
                            .symbol(symbol)
                            .chainType(chainType)
                            .priceUsd(price)
                            .recordedAt(now)
                            .build();

                    priceHistoryRepository.save(entity);
                    recordedCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to record price for {}/{}: {}", symbol, chainType, e.getMessage());
            }
        }

        log.info("Recorded {} token prices at {}", recordedCount, now);
    }

    /**
     * Get the 24h price change percentage for a token.
     * Falls back to mock data if historical data is not available.
     *
     * @param symbol The token symbol
     * @param chainType The chain type
     * @return The 24h change percentage, or BigDecimal.ZERO if unavailable
     */
    @Override
    public BigDecimal get24hChangePercent(String symbol, String chainType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);

        // Get price from ~24h ago
        Optional<TokenPriceHistoryEntity> historicalPriceOpt = priceHistoryRepository
                .findClosestPriceBefore(symbol.toUpperCase(), chainType.toUpperCase(), twentyFourHoursAgo);

        if (historicalPriceOpt.isEmpty()) {
            log.debug("No historical price found for {}/{}, falling back to mock", symbol, chainType);
            return priceService.get24hChangePercent(symbol, chainType);
        }

        BigDecimal historicalPrice = historicalPriceOpt.get().getPriceUsd();
        BigDecimal currentPrice = priceService.getPrice(symbol, chainType);

        if (historicalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Calculate: (currentPrice - historicalPrice) / historicalPrice * 100
        BigDecimal change = currentPrice.subtract(historicalPrice)
                .divide(historicalPrice, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("24h change for {}/{}: {}% (current: {}, 24h ago: {})",
                symbol, chainType, change, currentPrice, historicalPrice);

        return change;
    }

    /**
     * Get the price from approximately 24 hours ago.
     *
     * @param symbol The token symbol
     * @param chainType The chain type
     * @return The price from ~24h ago, or empty if not available
     */
    @Override
    public Optional<BigDecimal> getPrice24hAgo(String symbol, String chainType) {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        return priceHistoryRepository
                .findClosestPriceBefore(symbol.toUpperCase(), chainType.toUpperCase(), twentyFourHoursAgo)
                .map(TokenPriceHistoryEntity::getPriceUsd);
    }

    /**
     * Clean up old price records to prevent table bloat.
     * Deletes records older than the specified hours.
     */
    @Override
    @Transactional
    public int cleanupOldPrices(int hoursToKeep) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursToKeep);
        int deletedCount = priceHistoryRepository.deleteOlderThan(cutoffTime);
        log.info("Cleaned up {} old price records (older than {} hours)", deletedCount, hoursToKeep);
        return deletedCount;
    }

    /**
     * Check if we have sufficient historical data for accurate 24h calculations.
     */
    @Override
    public boolean hasHistoricalData(String symbol, String chainType) {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        return priceHistoryRepository
                .findClosestPriceBefore(symbol.toUpperCase(), chainType.toUpperCase(), twentyFourHoursAgo)
                .isPresent();
    }
}
