package org.mangala.wallet.price.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.price.service.PriceHistoryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for recording token prices periodically.
 * Records prices every 5 minutes for 24h change calculation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "portfolio.price.history-enabled", havingValue = "true", matchIfMissing = true)
public class PriceRecordingScheduler {

    private final PriceHistoryService priceHistoryService;

    /**
     * Record current prices every 5 minutes.
     * This ensures we have price data for accurate 24h change calculations.
     */
    @Scheduled(fixedRateString = "${portfolio.price.recording-interval-ms:300000}")
    public void recordPrices() {
        log.debug("Starting scheduled price recording");
        try {
            priceHistoryService.recordCurrentPrices();
        } catch (Exception e) {
            log.error("Failed to record prices: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up old price records every hour.
     * Keeps only the last 48 hours of data.
     */
    @Scheduled(cron = "${portfolio.price.cleanup-cron:0 0 * * * *}")
    public void cleanupOldPrices() {
        log.debug("Starting scheduled price cleanup");
        try {
            int deleted = priceHistoryService.cleanupOldPrices(48);
            if (deleted > 0) {
                log.info("Scheduled cleanup removed {} old price records", deleted);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup old prices: {}", e.getMessage(), e);
        }
    }
}
