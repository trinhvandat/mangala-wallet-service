package org.mangala.wallet.balance.sync;

/**
 * Result of a balance sync run, summarising counts and timing.
 */
public record SyncResult(
        int walletsProcessed,
        int walletsSucceeded,
        int walletsFailed,
        int tokensFound,
        long syncDurationMs
) {

    /** Convenience factory for an empty/zero result. */
    public static SyncResult empty() {
        return new SyncResult(0, 0, 0, 0, 0);
    }

    @Override
    public String toString() {
        return "SyncResult{walletsProcessed=%d, walletsSucceeded=%d, walletsFailed=%d, tokensFound=%d, syncDurationMs=%d}"
                .formatted(walletsProcessed, walletsSucceeded, walletsFailed, tokensFound, syncDurationMs);
    }
}
