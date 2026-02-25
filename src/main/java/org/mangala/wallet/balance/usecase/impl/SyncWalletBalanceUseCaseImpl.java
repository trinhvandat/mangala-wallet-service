package org.mangala.wallet.balance.usecase.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.balance.adapter.repository.WalletBalanceRepository;
import org.mangala.wallet.balance.adapter.web.dto.SyncResponse;
import org.mangala.wallet.balance.domain.WalletBalanceEntity;
import org.mangala.wallet.balance.usecase.SyncWalletBalanceUseCase;
import org.mangala.wallet.chain.adapter.ChainAdapter;
import org.mangala.wallet.chain.adapter.ChainAdapterResolver;
import org.mangala.wallet.chain.domain.ChainType;
import org.mangala.wallet.chain.domain.TokenBalance;
import org.mangala.wallet.shared.constant.ErrorConstant;
import org.mangala.wallet.shared.exception.WalletException;
import org.mangala.wallet.wallet.adapter.repository.WalletRepository;
import org.mangala.wallet.wallet.domain.WalletEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncWalletBalanceUseCaseImpl implements SyncWalletBalanceUseCase {

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final ChainAdapterResolver chainAdapterResolver;

    @Override
    @Transactional
    public SyncResponse execute(UUID walletId) {
        long startMs = System.currentTimeMillis();
        log.info("Starting manual sync for walletId={}", walletId);

        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletException(ErrorConstant.WALLET_NOT_FOUND));

        ChainType chainType;
        try {
            chainType = ChainType.valueOf(wallet.getChainType());
        } catch (IllegalArgumentException e) {
            log.error("Unsupported chain type '{}' for walletId={}", wallet.getChainType(), walletId);
            throw new WalletException(ErrorConstant.CHAIN_NOT_SUPPORTED);
        }

        ChainAdapter adapter = chainAdapterResolver.getAdapter(chainType)
                .orElseThrow(() -> new WalletException(ErrorConstant.CHAIN_NOT_SUPPORTED));

        if (!adapter.isAvailable()) {
            log.warn("Chain {} RPC unavailable during sync for walletId={}", chainType, walletId);
            throw new WalletException(ErrorConstant.CHAIN_UNAVAILABLE);
        }

        List<TokenBalance> tokenBalances;
        try {
            tokenBalances = adapter.getAllBalances(wallet.getAddress());
        } catch (Exception e) {
            log.error("Failed to fetch balances from chain for walletId={}: {}", walletId, e.getMessage(), e);
            long durationMs = System.currentTimeMillis() - startMs;
            return new SyncResponse(walletId, "FAILED", 0, durationMs,
                    "Failed to fetch balances from chain: " + e.getMessage());
        }

        LocalDateTime syncedAt = LocalDateTime.now();
        List<WalletBalanceEntity> toSave = new ArrayList<>();

        for (TokenBalance tb : tokenBalances) {
            String contractAddress = tb.isNativeToken() ? null : tb.getTokenAddress();

            Optional<WalletBalanceEntity> existing = contractAddress == null
                    ? walletBalanceRepository.findNativeBalance(walletId, wallet.getChainType())
                    : walletBalanceRepository.findByWalletIdAndChainTypeAndContractAddress(
                            walletId, wallet.getChainType(), contractAddress);

            WalletBalanceEntity entity = existing.map(e -> {
                e.setBalanceRaw(tb.getBalanceRaw().toString());
                e.setBalance(tb.getBalance());
                e.setLastSyncedAt(syncedAt);
                return e;
            }).orElseGet(() -> WalletBalanceEntity.builder()
                    .walletId(walletId)
                    .chainType(wallet.getChainType())
                    .contractAddress(contractAddress)
                    .symbol(tb.getSymbol())
                    .name(tb.getName())
                    .decimals(tb.getDecimals())
                    .balanceRaw(tb.getBalanceRaw().toString())
                    .balance(tb.getBalance())
                    .lastSyncedAt(syncedAt)
                    .build());

            toSave.add(entity);
        }

        walletBalanceRepository.saveAll(toSave);

        wallet.setLastSyncedAt(syncedAt);
        walletRepository.save(wallet);

        long durationMs = System.currentTimeMillis() - startMs;
        log.info("Sync completed for walletId={}: {} tokens found in {}ms", walletId, toSave.size(), durationMs);

        return new SyncResponse(walletId, "COMPLETED", toSave.size(), durationMs,
                "Sync completed successfully");
    }
}
