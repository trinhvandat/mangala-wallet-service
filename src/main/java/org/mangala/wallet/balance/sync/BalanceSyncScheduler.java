package org.mangala.wallet.balance.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.balance.health.BalanceSyncHealthIndicator;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Scheduled job that triggers a full balance sync every 5 minutes.
 *
 * <p>A Redisson distributed lock named {@value #LOCK_NAME} ensures that only one
 * service instance performs the sync at a time.  The lock TTL is set to
 * {@value #LOCK_TTL_MINUTES} minutes so it is automatically released even if the
 * JVM dies mid-run.
 *
 * <p>The scheduler uses a fixed-delay of 5 minutes ({@code 300_000 ms}) so that
 * consecutive runs never overlap regardless of how long each sync takes.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "balance.sync.enabled", havingValue = "true", matchIfMissing = true)
public class BalanceSyncScheduler {

    private static final String LOCK_NAME = "balance-sync-lock";
    private static final long LOCK_TTL_MINUTES = 10L;

    private final RedissonClient redissonClient;
    private final BalanceSyncService balanceSyncService;
    private final BalanceSyncHealthIndicator healthIndicator;

    /**
     * Runs every 5 minutes.  Acquires a distributed lock before delegating to
     * {@link BalanceSyncService#syncAll()}.  If another instance already holds
     * the lock the current run is skipped gracefully.
     */
    @Scheduled(fixedDelayString = "${balance.sync.interval-ms:300000}",
               initialDelayString = "${balance.sync.initial-delay-ms:10000}")
    public void scheduledSync() {
        log.debug("Balance sync scheduler triggered, attempting to acquire lock '{}'", LOCK_NAME);

        RLock lock = redissonClient.getLock(LOCK_NAME);
        boolean acquired = false;

        try {
            // tryLock(waitTime=0) returns false immediately if another instance holds it
            acquired = lock.tryLock(0, LOCK_TTL_MINUTES, TimeUnit.MINUTES);

            if (!acquired) {
                log.info("Balance sync skipped – lock '{}' already held by another instance", LOCK_NAME);
                return;
            }

            log.info("Balance sync lock acquired, starting sync");
            healthIndicator.recordSyncStart();

            SyncResult result = balanceSyncService.syncAll();

            // Record health status based on result
            if (result.walletsFailed() == 0) {
                healthIndicator.recordSuccess(result);
            } else if (result.walletsSucceeded() > 0) {
                // Partial success - still record as success but log warning
                healthIndicator.recordSuccess(result);
                log.warn("Balance sync completed with {} failures out of {} wallets",
                        result.walletsFailed(), result.walletsProcessed());
            } else {
                healthIndicator.recordFailure("All wallet syncs failed");
            }

            log.info("Balance sync finished: walletsProcessed={}, succeeded={}, failed={}, tokensFound={}, durationMs={}",
                    result.walletsProcessed(),
                    result.walletsSucceeded(),
                    result.walletsFailed(),
                    result.tokensFound(),
                    result.syncDurationMs());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Balance sync scheduler interrupted while acquiring lock", e);
            healthIndicator.recordFailure("Interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error("Balance sync failed with unexpected error", e);
            healthIndicator.recordFailure(e.getMessage());
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Balance sync lock '{}' released", LOCK_NAME);
            }
        }
    }
}
