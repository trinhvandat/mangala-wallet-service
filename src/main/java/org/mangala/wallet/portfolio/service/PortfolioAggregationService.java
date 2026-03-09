package org.mangala.wallet.portfolio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.balance.adapter.repository.WalletBalanceRepository;
import org.mangala.wallet.balance.domain.WalletBalanceEntity;
import org.mangala.wallet.balance.event.BalanceUpdateEvent;
import org.mangala.wallet.portfolio.adapter.repository.UserPortfolioBalanceRepository;
import org.mangala.wallet.portfolio.domain.UserPortfolioBalanceEntity;
import org.mangala.wallet.wallet.adapter.repository.WalletRepository;
import org.mangala.wallet.wallet.domain.WalletEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for aggregating wallet balances into portfolio view.
 * Processes balance update events and maintains aggregated portfolio data per user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioAggregationService {

    private final UserPortfolioBalanceRepository portfolioRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final WalletRepository walletRepository;

    /**
     * Process a balance update event and update aggregated portfolio balances.
     *
     * @param event The balance update event from Kafka
     */
    @Transactional
    public void processBalanceUpdate(BalanceUpdateEvent event) {
        log.debug("Processing balance update for wallet: {}", event.getWalletId());

        // Get the wallet to find the user
        Optional<WalletEntity> walletOpt = walletRepository.findById(event.getWalletId());
        if (walletOpt.isEmpty()) {
            log.warn("Wallet not found for balance update: {}", event.getWalletId());
            return;
        }

        WalletEntity wallet = walletOpt.get();
        UUID userId = wallet.getUserId();
        String chainType = event.getChain().toUpperCase();

        // Process each token balance from the event
        for (BalanceUpdateEvent.TokenBalancePayload balance : event.getBalances()) {
            aggregateTokenBalance(userId, chainType, balance);
        }

        log.debug("Completed portfolio aggregation for user {} with {} tokens",
                userId, event.getBalances().size());
    }

    /**
     * Recalculate entire portfolio for a user.
     * Useful for initial load or full reconciliation.
     *
     * @param userId The user ID to recalculate for
     */
    @Transactional
    public void recalculateUserPortfolio(UUID userId) {
        log.info("Recalculating portfolio for user: {}", userId);

        // Get all wallets for the user
        List<WalletEntity> userWallets = walletRepository.findByUserIdAndIsActiveTrue(userId);
        if (userWallets.isEmpty()) {
            log.debug("No active wallets found for user: {}", userId);
            return;
        }

        // Get all balances for user's wallets
        List<UUID> walletIds = userWallets.stream()
                .map(WalletEntity::getId)
                .toList();

        // Group balances by chain + contract address
        Map<String, AggregatedBalance> aggregations = new HashMap<>();

        for (UUID walletId : walletIds) {
            List<WalletBalanceEntity> balances = walletBalanceRepository.findByWalletId(walletId);
            for (WalletBalanceEntity balance : balances) {
                String key = buildAggregationKey(balance.getChainType(), balance.getContractAddress());

                aggregations.computeIfAbsent(key, k -> new AggregatedBalance(
                        balance.getChainType(),
                        balance.getContractAddress(),
                        balance.getSymbol(),
                        balance.getName()
                )).addBalance(balance.getBalance());
            }
        }

        // Update portfolio balances
        for (AggregatedBalance agg : aggregations.values()) {
            upsertPortfolioBalance(userId, agg);
        }

        log.info("Completed portfolio recalculation for user {} with {} tokens",
                userId, aggregations.size());
    }

    /**
     * Get the aggregated portfolio summary for a user.
     *
     * @param userId The user ID
     * @return List of portfolio balance entities
     */
    public List<UserPortfolioBalanceEntity> getUserPortfolio(UUID userId) {
        return portfolioRepository.findNonZeroBalancesByUserId(userId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void aggregateTokenBalance(UUID userId, String chainType,
                                       BalanceUpdateEvent.TokenBalancePayload balance) {
        String contractAddress = balance.getContractAddress();
        BigDecimal amount = new BigDecimal(balance.getAmount());

        // For events, we need to recalculate from all wallets
        // This ensures accuracy when a wallet's balance changes
        recalculateTokenBalance(userId, chainType, contractAddress,
                balance.getSymbol(), null);
    }

    private void recalculateTokenBalance(UUID userId, String chainType,
                                         String contractAddress, String symbol, String name) {
        // Get all wallets for user
        List<WalletEntity> userWallets = walletRepository.findByUserIdAndIsActiveTrue(userId);
        List<UUID> walletIds = userWallets.stream()
                .filter(w -> w.getChainType().equalsIgnoreCase(chainType))
                .map(WalletEntity::getId)
                .toList();

        if (walletIds.isEmpty()) {
            return;
        }

        // Sum balances across all wallets for this token
        BigDecimal totalBalance = BigDecimal.ZERO;
        int walletCount = 0;
        String tokenName = name;
        String tokenSymbol = symbol;

        for (UUID walletId : walletIds) {
            Optional<WalletBalanceEntity> balanceOpt = contractAddress == null
                    ? walletBalanceRepository.findNativeBalance(walletId, chainType)
                    : walletBalanceRepository.findByWalletIdAndChainTypeAndContractAddress(
                            walletId, chainType, contractAddress);

            if (balanceOpt.isPresent()) {
                WalletBalanceEntity bal = balanceOpt.get();
                totalBalance = totalBalance.add(bal.getBalance());
                walletCount++;
                if (tokenName == null) {
                    tokenName = bal.getName();
                }
                if (tokenSymbol == null) {
                    tokenSymbol = bal.getSymbol();
                }
            }
        }

        // Upsert the aggregated balance
        AggregatedBalance agg = new AggregatedBalance(chainType, contractAddress,
                tokenSymbol != null ? tokenSymbol : "UNKNOWN", tokenName);
        agg.setTotalBalance(totalBalance);
        agg.setWalletCount(walletCount);

        upsertPortfolioBalance(userId, agg);
    }

    private void upsertPortfolioBalance(UUID userId, AggregatedBalance agg) {
        Optional<UserPortfolioBalanceEntity> existing = agg.getContractAddress() == null
                ? portfolioRepository.findNativeTokenBalance(userId, agg.getChainType())
                : portfolioRepository.findByUserIdAndChainTypeAndContractAddress(
                        userId, agg.getChainType(), agg.getContractAddress());

        if (existing.isPresent()) {
            UserPortfolioBalanceEntity entity = existing.get();
            entity.setTotalBalance(agg.getTotalBalance());
            entity.setWalletCount(agg.getWalletCount());
            entity.setLastUpdatedAt(LocalDateTime.now());
            portfolioRepository.save(entity);
        } else if (agg.getTotalBalance().compareTo(BigDecimal.ZERO) > 0) {
            UserPortfolioBalanceEntity entity = UserPortfolioBalanceEntity.builder()
                    .userId(userId)
                    .chainType(agg.getChainType())
                    .contractAddress(agg.getContractAddress())
                    .symbol(agg.getSymbol())
                    .name(agg.getName())
                    .totalBalance(agg.getTotalBalance())
                    .walletCount(agg.getWalletCount())
                    .build();
            portfolioRepository.save(entity);
        }
    }

    private String buildAggregationKey(String chainType, String contractAddress) {
        return chainType + ":" + (contractAddress != null ? contractAddress : "NATIVE");
    }

    /**
     * Helper class for aggregating balances during calculation.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class AggregatedBalance {
        private final String chainType;
        private final String contractAddress;
        private final String symbol;
        private final String name;
        private BigDecimal totalBalance = BigDecimal.ZERO;
        private int walletCount = 0;

        AggregatedBalance(String chainType, String contractAddress, String symbol, String name) {
            this.chainType = chainType;
            this.contractAddress = contractAddress;
            this.symbol = symbol;
            this.name = name;
        }

        void addBalance(BigDecimal amount) {
            this.totalBalance = this.totalBalance.add(amount);
            this.walletCount++;
        }
    }
}
