package org.mangala.wallet.portfolio.usecase.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mangala.wallet.portfolio.adapter.repository.UserPortfolioBalanceRepository;
import org.mangala.wallet.portfolio.adapter.web.dto.PortfolioSummaryResponse;
import org.mangala.wallet.portfolio.adapter.web.dto.TokenHolding;
import org.mangala.wallet.portfolio.domain.UserPortfolioBalanceEntity;
import org.mangala.wallet.price.PriceService;
import org.mangala.wallet.price.service.PriceHistoryProvider;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPortfolioSummaryUseCaseImpl")
class GetPortfolioSummaryUseCaseImplTest {

    @Mock
    private UserPortfolioBalanceRepository portfolioRepository;

    @Mock
    private PriceService priceService;

    @Mock
    private PriceHistoryProvider priceHistoryService;

    @InjectMocks
    private GetPortfolioSummaryUseCaseImpl useCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("getPortfolioSummary")
    class GetPortfolioSummary {

        @Test
        @DisplayName("should return empty portfolio when user has no balances")
        void shouldReturnEmptyPortfolioWhenNoBalances() {
            when(portfolioRepository.findNonZeroBalancesByUserId(userId))
                    .thenReturn(Collections.emptyList());

            PortfolioSummaryResponse result = useCase.getPortfolioSummary(userId);

            assertThat(result.getTotalValueUsd()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTokens()).isEmpty();
            assertThat(result.getChainSummary()).isEmpty();
            assertThat(result.getChange24h().getAmountUsd()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getChange24h().getPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should calculate total value correctly with single token")
        void shouldCalculateTotalValueWithSingleToken() {
            UserPortfolioBalanceEntity ethBalance = createBalance(
                    "ETHEREUM", null, "ETH", "Ethereum",
                    new BigDecimal("2.5"), 1);

            when(portfolioRepository.findNonZeroBalancesByUserId(userId))
                    .thenReturn(List.of(ethBalance));
            when(priceService.getPrice("ETH", "ETHEREUM"))
                    .thenReturn(new BigDecimal("3500.00"));
            when(priceHistoryService.get24hChangePercent("ETH", "ETHEREUM"))
                    .thenReturn(new BigDecimal("2.5"));

            PortfolioSummaryResponse result = useCase.getPortfolioSummary(userId);

            // 2.5 ETH * $3500 = $8750
            assertThat(result.getTotalValueUsd()).isEqualByComparingTo(new BigDecimal("8750.00"));
            assertThat(result.getTokens()).hasSize(1);
            assertThat(result.getChainSummary()).containsKey("ETHEREUM");
            assertThat(result.getChainSummary().get("ETHEREUM"))
                    .isEqualByComparingTo(new BigDecimal("8750.00"));
        }

        @Test
        @DisplayName("should calculate total value correctly with multiple tokens")
        void shouldCalculateTotalValueWithMultipleTokens() {
            UserPortfolioBalanceEntity ethBalance = createBalance(
                    "ETHEREUM", null, "ETH", "Ethereum",
                    new BigDecimal("1.0"), 1);
            UserPortfolioBalanceEntity bnbBalance = createBalance(
                    "BSC", null, "BNB", "BNB",
                    new BigDecimal("10.0"), 2);

            when(portfolioRepository.findNonZeroBalancesByUserId(userId))
                    .thenReturn(List.of(ethBalance, bnbBalance));
            when(priceService.getPrice("ETH", "ETHEREUM"))
                    .thenReturn(new BigDecimal("3500.00"));
            when(priceService.getPrice("BNB", "BSC"))
                    .thenReturn(new BigDecimal("580.00"));
            when(priceHistoryService.get24hChangePercent(anyString(), anyString()))
                    .thenReturn(BigDecimal.ZERO);

            PortfolioSummaryResponse result = useCase.getPortfolioSummary(userId);

            // 1 ETH * $3500 + 10 BNB * $580 = $3500 + $5800 = $9300
            assertThat(result.getTotalValueUsd()).isEqualByComparingTo(new BigDecimal("9300.00"));
            assertThat(result.getTokens()).hasSize(2);
            assertThat(result.getChainSummary()).containsKeys("ETHEREUM", "BSC");
        }

        @Test
        @DisplayName("should calculate allocation percentages correctly")
        void shouldCalculateAllocationPercentages() {
            UserPortfolioBalanceEntity ethBalance = createBalance(
                    "ETHEREUM", null, "ETH", "Ethereum",
                    new BigDecimal("1.0"), 1);
            UserPortfolioBalanceEntity usdtBalance = createBalance(
                    "ETHEREUM", "0xdAC17F958D2ee523a2206206994597C13D831ec7",
                    "USDT", "Tether",
                    new BigDecimal("1000.0"), 1);

            when(portfolioRepository.findNonZeroBalancesByUserId(userId))
                    .thenReturn(List.of(ethBalance, usdtBalance));
            when(priceService.getPrice("ETH", "ETHEREUM"))
                    .thenReturn(new BigDecimal("4000.00"));
            when(priceService.getPrice("USDT", "ETHEREUM"))
                    .thenReturn(new BigDecimal("1.00"));
            when(priceHistoryService.get24hChangePercent(anyString(), anyString()))
                    .thenReturn(BigDecimal.ZERO);

            PortfolioSummaryResponse result = useCase.getPortfolioSummary(userId);

            // Total = $4000 + $1000 = $5000
            // ETH allocation = 4000/5000 = 80%
            // USDT allocation = 1000/5000 = 20%
            assertThat(result.getTotalValueUsd()).isEqualByComparingTo(new BigDecimal("5000.00"));

            TokenHolding ethHolding = result.getTokens().stream()
                    .filter(t -> "ETH".equals(t.getSymbol()))
                    .findFirst().orElseThrow();
            assertThat(ethHolding.getAllocationPercent()).isEqualByComparingTo(new BigDecimal("80.00"));

            TokenHolding usdtHolding = result.getTokens().stream()
                    .filter(t -> "USDT".equals(t.getSymbol()))
                    .findFirst().orElseThrow();
            assertThat(usdtHolding.getAllocationPercent()).isEqualByComparingTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("should sort tokens by value descending")
        void shouldSortTokensByValueDescending() {
            UserPortfolioBalanceEntity smallBalance = createBalance(
                    "ETHEREUM", null, "ETH", "Ethereum",
                    new BigDecimal("0.1"), 1);
            UserPortfolioBalanceEntity largeBalance = createBalance(
                    "BSC", null, "BNB", "BNB",
                    new BigDecimal("100.0"), 1);

            when(portfolioRepository.findNonZeroBalancesByUserId(userId))
                    .thenReturn(List.of(smallBalance, largeBalance));
            when(priceService.getPrice("ETH", "ETHEREUM"))
                    .thenReturn(new BigDecimal("3500.00")); // $350
            when(priceService.getPrice("BNB", "BSC"))
                    .thenReturn(new BigDecimal("580.00")); // $58000
            when(priceHistoryService.get24hChangePercent(anyString(), anyString()))
                    .thenReturn(BigDecimal.ZERO);

            PortfolioSummaryResponse result = useCase.getPortfolioSummary(userId);

            assertThat(result.getTokens()).hasSize(2);
            assertThat(result.getTokens().get(0).getSymbol()).isEqualTo("BNB");
            assertThat(result.getTokens().get(1).getSymbol()).isEqualTo("ETH");
        }
    }

    @Nested
    @DisplayName("getPortfolioSummaryByChain")
    class GetPortfolioSummaryByChain {

        @Test
        @DisplayName("should filter by chain type")
        void shouldFilterByChainType() {
            UserPortfolioBalanceEntity ethBalance = createBalance(
                    "ETHEREUM", null, "ETH", "Ethereum",
                    new BigDecimal("1.0"), 1);

            when(portfolioRepository.findByUserIdAndChainType(userId, "ETHEREUM"))
                    .thenReturn(List.of(ethBalance));
            when(priceService.getPrice("ETH", "ETHEREUM"))
                    .thenReturn(new BigDecimal("3500.00"));
            when(priceHistoryService.get24hChangePercent("ETH", "ETHEREUM"))
                    .thenReturn(BigDecimal.ZERO);

            PortfolioSummaryResponse result = useCase.getPortfolioSummaryByChain(userId, "ETHEREUM");

            assertThat(result.getTokens()).hasSize(1);
            assertThat(result.getTokens().get(0).getChain()).isEqualTo("ETHEREUM");
            assertThat(result.getChainSummary()).containsOnlyKeys("ETHEREUM");
        }
    }

    private UserPortfolioBalanceEntity createBalance(String chainType, String contractAddress,
                                                      String symbol, String name,
                                                      BigDecimal totalBalance, int walletCount) {
        return UserPortfolioBalanceEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .chainType(chainType)
                .contractAddress(contractAddress)
                .symbol(symbol)
                .name(name)
                .totalBalance(totalBalance)
                .walletCount(walletCount)
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }
}
