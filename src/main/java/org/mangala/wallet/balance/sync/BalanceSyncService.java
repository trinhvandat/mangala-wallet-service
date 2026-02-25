package org.mangala.wallet.balance.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.balance.adapter.repository.WalletBalanceRepository;
import org.mangala.wallet.balance.domain.WalletBalanceEntity;
import org.mangala.wallet.balance.event.KafkaBalancePublisher;
import org.mangala.wallet.chain.adapter.ChainAdapter;
import org.mangala.wallet.chain.adapter.ChainAdapterResolver;
import org.mangala.wallet.chain.domain.ChainType;
import org.mangala.wallet.chain.domain.TokenBalance;
import org.mangala.wallet.wallet.adapter.repository.WalletRepository;
import org.mangala.wallet.wallet.domain.WalletEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performs the actual balance synchronisation work.
 *
 * <p>Wallets are fetched in batches of {@value #BATCH_SIZE}.  For each wallet the
 * appropriate {@link ChainAdapter} is resolved and
 * {@link ChainAdapter#getTokenBalances(String, List)} is called.  Results are
 * persisted via an upsert against {@link WalletBalanceRepository} and
 * {@link WalletEntity#getLastSyncedAt()} is updated.
 *
 * <p>Each wallet is retried up to {@value #MAX_ATTEMPTS} times using exponential
 * back-off starting at {@value #INITIAL_DELAY_MS} ms.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceSyncService {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_DELAY_MS = 1_000L;

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final ChainAdapterResolver chainAdapterResolver;
    private final KafkaBalancePublisher kafkaBalancePublisher;

    /**
     * Synchronises balances for every active wallet and returns a {@link SyncResult}
     * describing the outcome.
     */
    public SyncResult syncAll() {
        long startMs = System.currentTimeMillis();

        List<WalletEntity> allWallets = walletRepository.findAllByIsActiveTrue();
        log.info("Balance sync starting: {} active wallets to process", allWallets.size());

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger tokensFound = new AtomicInteger();

        // Process in batches of BATCH_SIZE
        for (int offset = 0; offset < allWallets.size(); offset += BATCH_SIZE) {
            int end = Math.min(offset + BATCH_SIZE, allWallets.size());
            List<WalletEntity> batch = allWallets.subList(offset, end);
            log.debug("Processing batch [{}-{}] of {}", offset, end - 1, allWallets.size());

            for (WalletEntity wallet : batch) {
                try {
                    int tokens = syncWalletWithRetry(wallet);
                    tokensFound.addAndGet(tokens);
                    succeeded.incrementAndGet();
                } catch (Exception e) {
                    log.error("Failed to sync wallet id={} address={} after {} attempts: {}",
                            wallet.getId(), wallet.getAddress(), MAX_ATTEMPTS, e.getMessage());
                    failed.incrementAndGet();
                }
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        SyncResult result = new SyncResult(
                allWallets.size(),
                succeeded.get(),
                failed.get(),
                tokensFound.get(),
                durationMs
        );
        log.info("Balance sync complete: {}", result);
        return result;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Syncs a single wallet, retrying up to {@value #MAX_ATTEMPTS} times with
     * exponential back-off.  Returns the number of token balances persisted.
     */
    private int syncWalletWithRetry(WalletEntity wallet) throws Exception {
        Exception lastException = null;
        long delayMs = INITIAL_DELAY_MS;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return doSyncWallet(wallet);
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_ATTEMPTS) {
                    log.warn("Sync attempt {}/{} failed for wallet id={}: {}. Retrying in {}ms",
                            attempt, MAX_ATTEMPTS, wallet.getId(), e.getMessage(), delayMs);
                    sleep(delayMs);
                    delayMs *= 2; // exponential back-off
                }
            }
        }

        throw lastException;
    }

    /**
     * Performs a single sync attempt for one wallet: fetches balances from the
     * chain and upserts each token balance into the repository.
     */
    @Transactional
    protected int doSyncWallet(WalletEntity wallet) {
        ChainType chainType = ChainType.valueOf(wallet.getChainType());

        Optional<ChainAdapter> adapterOpt = chainAdapterResolver.getAdapter(chainType);
        if (adapterOpt.isEmpty()) {
            log.warn("No chain adapter available for chainType={}, skipping wallet id={}",
                    chainType, wallet.getId());
            return 0;
        }

        ChainAdapter adapter = adapterOpt.get();
        List<TokenBalance> balances = adapter.getTokenBalances(wallet.getAddress(), List.of());

        LocalDateTime syncedAt = LocalDateTime.now();

        for (TokenBalance tb : balances) {
            upsertBalance(wallet, chainType, tb, syncedAt);
        }

        // Update the wallet's lastSyncedAt timestamp
        wallet.setLastSyncedAt(syncedAt);
        walletRepository.save(wallet);

        // Publish balance update event to Kafka
        List<WalletBalanceEntity> savedBalances = walletBalanceRepository.findByWalletId(wallet.getId());
        kafkaBalancePublisher.publishBalanceUpdate(wallet, savedBalances);

        log.debug("Synced wallet id={} address={} chain={}: {} token(s)",
                wallet.getId(), wallet.getAddress(), chainType, balances.size());

        return balances.size();
    }

    /**
     * Upserts a single {@link TokenBalance} for the given wallet.
     * Native-token entries are keyed on (walletId, chainType, contractAddress=NULL).
     */
    private void upsertBalance(WalletEntity wallet,
                               ChainType chainType,
                               TokenBalance tb,
                               LocalDateTime syncedAt) {

        String contractAddress = tb.isNativeToken() ? null : tb.getTokenAddress();

        Optional<WalletBalanceEntity> existing = tb.isNativeToken()
                ? walletBalanceRepository.findNativeBalance(wallet.getId(), chainType.name())
                : walletBalanceRepository.findByWalletIdAndChainTypeAndContractAddress(
                        wallet.getId(), chainType.name(), contractAddress);

        if (existing.isPresent()) {
            WalletBalanceEntity entity = existing.get();
            entity.setBalanceRaw(tb.getBalanceRaw().toString());
            entity.setBalance(tb.getBalance());
            entity.setLastSyncedAt(syncedAt);
            walletBalanceRepository.save(entity);
        } else {
            WalletBalanceEntity entity = WalletBalanceEntity.builder()
                    .walletId(wallet.getId())
                    .chainType(chainType.name())
                    .contractAddress(contractAddress)
                    .symbol(tb.getSymbol())
                    .name(tb.getName())
                    .decimals(tb.getDecimals())
                    .balanceRaw(tb.getBalanceRaw().toString())
                    .balance(tb.getBalance())
                    .lastSyncedAt(syncedAt)
                    .build();
            walletBalanceRepository.save(entity);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted during retry back-off");
        }
    }
}
