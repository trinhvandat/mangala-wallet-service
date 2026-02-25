package org.mangala.wallet.balance.adapter.repository;

import org.mangala.wallet.balance.domain.WalletBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for wallet balance persistence operations.
 */
@Repository
public interface WalletBalanceRepository extends JpaRepository<WalletBalanceEntity, UUID> {

    /**
     * Find all balances for a specific wallet.
     */
    List<WalletBalanceEntity> findByWalletId(UUID walletId);

    /**
     * Find all non-zero balances for a wallet.
     */
    @Query("SELECT wb FROM WalletBalanceEntity wb WHERE wb.walletId = :walletId AND wb.balance > 0")
    List<WalletBalanceEntity> findNonZeroBalancesByWalletId(@Param("walletId") UUID walletId);

    /**
     * Find a specific token balance for a wallet.
     */
    Optional<WalletBalanceEntity> findByWalletIdAndChainTypeAndContractAddress(
            UUID walletId, String chainType, String contractAddress);

    /**
     * Find native token balance for a wallet (contract_address is NULL).
     */
    @Query("SELECT wb FROM WalletBalanceEntity wb WHERE wb.walletId = :walletId AND wb.chainType = :chainType AND wb.contractAddress IS NULL")
    Optional<WalletBalanceEntity> findNativeBalance(
            @Param("walletId") UUID walletId,
            @Param("chainType") String chainType);

    /**
     * Find wallets with stale balances (not synced within given threshold).
     */
    @Query("SELECT DISTINCT wb.walletId FROM WalletBalanceEntity wb WHERE wb.lastSyncedAt < :threshold")
    List<UUID> findWalletsWithStaleBalances(@Param("threshold") LocalDateTime threshold);

    /**
     * Delete all balances for a wallet.
     */
    @Modifying
    @Query("DELETE FROM WalletBalanceEntity wb WHERE wb.walletId = :walletId")
    void deleteByWalletId(@Param("walletId") UUID walletId);

    /**
     * Count total balances for a wallet.
     */
    long countByWalletId(UUID walletId);
}
