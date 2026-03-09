package org.mangala.wallet.balance.health;

import lombok.RequiredArgsConstructor;
import org.mangala.wallet.balance.sync.SyncResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Health indicator for balance synchronization.
 * Reports the status of the last sync operation and tracks failure counts.
 */
@Component
@RequiredArgsConstructor
public class BalanceSyncHealthIndicator implements HealthIndicator {

    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private final AtomicReference<LocalDateTime> lastSuccessfulSync = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> lastSyncAttempt = new AtomicReference<>();
    private final AtomicReference<SyncResult> lastSyncResult = new AtomicReference<>();
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<String> lastError = new AtomicReference<>();

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        LocalDateTime lastSuccess = lastSuccessfulSync.get();
        LocalDateTime lastAttempt = lastSyncAttempt.get();
        SyncResult lastResult = lastSyncResult.get();
        int failures = consecutiveFailures.get();

        // Add details
        if (lastSuccess != null) {
            builder.withDetail("lastSuccessfulSync", lastSuccess.toString());
        }
        if (lastAttempt != null) {
            builder.withDetail("lastSyncAttempt", lastAttempt.toString());
        }
        if (lastResult != null) {
            builder.withDetail("lastSyncResult", formatSyncResult(lastResult));
        }
        builder.withDetail("consecutiveFailures", failures);

        // Determine health status
        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            builder.down()
                    .withDetail("status", "Multiple consecutive sync failures")
                    .withDetail("lastError", lastError.get());
        } else if (lastSuccess == null && lastAttempt != null) {
            builder.unknown()
                    .withDetail("status", "No successful sync yet");
        } else if (failures > 0) {
            builder.status("DEGRADED")
                    .withDetail("status", "Recent sync failure")
                    .withDetail("lastError", lastError.get());
        } else {
            builder.withDetail("status", "Healthy");
        }

        return builder.build();
    }

    /**
     * Record a successful sync operation.
     */
    public void recordSuccess(SyncResult result) {
        LocalDateTime now = LocalDateTime.now();
        lastSuccessfulSync.set(now);
        lastSyncAttempt.set(now);
        lastSyncResult.set(result);
        consecutiveFailures.set(0);
        lastError.set(null);
    }

    /**
     * Record a failed sync operation.
     */
    public void recordFailure(String errorMessage) {
        lastSyncAttempt.set(LocalDateTime.now());
        consecutiveFailures.incrementAndGet();
        lastError.set(errorMessage);
    }

    /**
     * Record a sync attempt start.
     */
    public void recordSyncStart() {
        lastSyncAttempt.set(LocalDateTime.now());
    }

    /**
     * Get the number of consecutive failures.
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /**
     * Check if sync is healthy (fewer than max consecutive failures).
     */
    public boolean isHealthy() {
        return consecutiveFailures.get() < MAX_CONSECUTIVE_FAILURES;
    }

    private String formatSyncResult(SyncResult result) {
        return String.format("total=%d, succeeded=%d, failed=%d, tokens=%d, duration=%dms",
                result.walletsProcessed(),
                result.walletsSucceeded(),
                result.walletsFailed(),
                result.tokensFound(),
                result.syncDurationMs());
    }
}
