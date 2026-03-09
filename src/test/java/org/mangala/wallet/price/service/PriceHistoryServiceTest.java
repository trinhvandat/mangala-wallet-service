package org.mangala.wallet.price.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mangala.wallet.price.PriceService;
import org.mangala.wallet.price.adapter.repository.TokenPriceHistoryRepository;
import org.mangala.wallet.price.domain.TokenPriceHistoryEntity;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceHistoryService")
class PriceHistoryServiceTest {

    @Mock
    private TokenPriceHistoryRepository priceHistoryRepository;

    @Mock
    private PriceService priceService;

    @InjectMocks
    private PriceHistoryService priceHistoryService;

    @Nested
    @DisplayName("get24hChangePercent")
    class Get24hChangePercent {

        @Test
        @DisplayName("should calculate 24h change from historical data")
        void shouldCalculate24hChangeFromHistoricalData() {
            // Given: ETH was $3000 24h ago, now $3500
            TokenPriceHistoryEntity historicalPrice = TokenPriceHistoryEntity.builder()
                    .symbol("ETH")
                    .chainType("ETHEREUM")
                    .priceUsd(new BigDecimal("3000.00"))
                    .recordedAt(LocalDateTime.now().minusHours(24))
                    .build();

            when(priceHistoryRepository.findClosestPriceBefore(eq("ETH"), eq("ETHEREUM"), any()))
                    .thenReturn(Optional.of(historicalPrice));
            when(priceService.getPrice("ETH", "ETHEREUM"))
                    .thenReturn(new BigDecimal("3500.00"));

            // When
            BigDecimal change = priceHistoryService.get24hChangePercent("ETH", "ETHEREUM");

            // Then: (3500 - 3000) / 3000 * 100 = 16.67%
            assertThat(change).isEqualByComparingTo(new BigDecimal("16.67"));
        }

        @Test
        @DisplayName("should handle negative price change")
        void shouldHandleNegativePriceChange() {
            // Given: ETH was $4000 24h ago, now $3500
            TokenPriceHistoryEntity historicalPrice = TokenPriceHistoryEntity.builder()
                    .symbol("ETH")
                    .chainType("ETHEREUM")
                    .priceUsd(new BigDecimal("4000.00"))
                    .recordedAt(LocalDateTime.now().minusHours(24))
                    .build();

            when(priceHistoryRepository.findClosestPriceBefore(eq("ETH"), eq("ETHEREUM"), any()))
                    .thenReturn(Optional.of(historicalPrice));
            when(priceService.getPrice("ETH", "ETHEREUM"))
                    .thenReturn(new BigDecimal("3500.00"));

            // When
            BigDecimal change = priceHistoryService.get24hChangePercent("ETH", "ETHEREUM");

            // Then: (3500 - 4000) / 4000 * 100 = -12.50%
            assertThat(change).isEqualByComparingTo(new BigDecimal("-12.50"));
        }

        @Test
        @DisplayName("should fall back to mock when no historical data")
        void shouldFallBackToMockWhenNoHistoricalData() {
            when(priceHistoryRepository.findClosestPriceBefore(eq("ETH"), eq("ETHEREUM"), any()))
                    .thenReturn(Optional.empty());
            when(priceService.get24hChangePercent("ETH", "ETHEREUM"))
                    .thenReturn(new BigDecimal("2.5"));

            BigDecimal change = priceHistoryService.get24hChangePercent("ETH", "ETHEREUM");

            assertThat(change).isEqualByComparingTo(new BigDecimal("2.5"));
            verify(priceService).get24hChangePercent("ETH", "ETHEREUM");
        }

        @Test
        @DisplayName("should handle zero historical price gracefully")
        void shouldHandleZeroHistoricalPriceGracefully() {
            TokenPriceHistoryEntity historicalPrice = TokenPriceHistoryEntity.builder()
                    .symbol("TEST")
                    .chainType("ETHEREUM")
                    .priceUsd(BigDecimal.ZERO)
                    .recordedAt(LocalDateTime.now().minusHours(24))
                    .build();

            when(priceHistoryRepository.findClosestPriceBefore(eq("TEST"), eq("ETHEREUM"), any()))
                    .thenReturn(Optional.of(historicalPrice));

            BigDecimal change = priceHistoryService.get24hChangePercent("TEST", "ETHEREUM");

            assertThat(change).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should be case insensitive")
        void shouldBeCaseInsensitive() {
            TokenPriceHistoryEntity historicalPrice = TokenPriceHistoryEntity.builder()
                    .symbol("ETH")
                    .chainType("ETHEREUM")
                    .priceUsd(new BigDecimal("3500.00"))
                    .recordedAt(LocalDateTime.now().minusHours(24))
                    .build();

            when(priceHistoryRepository.findClosestPriceBefore(eq("ETH"), eq("ETHEREUM"), any()))
                    .thenReturn(Optional.of(historicalPrice));
            when(priceService.getPrice("eth", "ethereum"))
                    .thenReturn(new BigDecimal("3500.00"));

            BigDecimal change = priceHistoryService.get24hChangePercent("eth", "ethereum");

            assertThat(change).isNotNull();
            verify(priceHistoryRepository).findClosestPriceBefore(eq("ETH"), eq("ETHEREUM"), any());
        }
    }

    @Nested
    @DisplayName("recordCurrentPrices")
    class RecordCurrentPrices {

        @Test
        @DisplayName("should record prices for tracked tokens")
        void shouldRecordPricesForTrackedTokens() {
            when(priceService.getPrice(anyString(), anyString()))
                    .thenReturn(new BigDecimal("100.00"));

            priceHistoryService.recordCurrentPrices();

            // Should save at least one price record
            verify(priceHistoryRepository, atLeastOnce()).save(any(TokenPriceHistoryEntity.class));
        }

        @Test
        @DisplayName("should not record zero prices")
        void shouldNotRecordZeroPrices() {
            when(priceService.getPrice(anyString(), anyString()))
                    .thenReturn(BigDecimal.ZERO);

            priceHistoryService.recordCurrentPrices();

            verify(priceHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should continue recording on individual token failure")
        void shouldContinueRecordingOnIndividualTokenFailure() {
            when(priceService.getPrice("ETH", "ETHEREUM"))
                    .thenThrow(new RuntimeException("API error"));
            when(priceService.getPrice(argThat(s -> !"ETH".equals(s)), anyString()))
                    .thenReturn(new BigDecimal("100.00"));

            priceHistoryService.recordCurrentPrices();

            // Should still save other token prices
            verify(priceHistoryRepository, atLeastOnce()).save(any());
        }
    }

    @Nested
    @DisplayName("cleanupOldPrices")
    class CleanupOldPrices {

        @Test
        @DisplayName("should delete records older than specified hours")
        void shouldDeleteRecordsOlderThanSpecifiedHours() {
            when(priceHistoryRepository.deleteOlderThan(any()))
                    .thenReturn(100);

            int deleted = priceHistoryService.cleanupOldPrices(48);

            assertThat(deleted).isEqualTo(100);

            ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(priceHistoryRepository).deleteOlderThan(timeCaptor.capture());

            LocalDateTime cutoff = timeCaptor.getValue();
            assertThat(cutoff).isBefore(LocalDateTime.now().minusHours(47));
            assertThat(cutoff).isAfter(LocalDateTime.now().minusHours(49));
        }
    }

    @Nested
    @DisplayName("hasHistoricalData")
    class HasHistoricalData {

        @Test
        @DisplayName("should return true when historical data exists")
        void shouldReturnTrueWhenHistoricalDataExists() {
            TokenPriceHistoryEntity entity = TokenPriceHistoryEntity.builder()
                    .priceUsd(new BigDecimal("100"))
                    .build();

            when(priceHistoryRepository.findClosestPriceBefore(eq("ETH"), eq("ETHEREUM"), any()))
                    .thenReturn(Optional.of(entity));

            assertThat(priceHistoryService.hasHistoricalData("ETH", "ETHEREUM")).isTrue();
        }

        @Test
        @DisplayName("should return false when no historical data")
        void shouldReturnFalseWhenNoHistoricalData() {
            when(priceHistoryRepository.findClosestPriceBefore(eq("ETH"), eq("ETHEREUM"), any()))
                    .thenReturn(Optional.empty());

            assertThat(priceHistoryService.hasHistoricalData("ETH", "ETHEREUM")).isFalse();
        }
    }
}
