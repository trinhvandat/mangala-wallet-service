package org.mangala.wallet.price.adapter.repository;

import org.mangala.wallet.price.domain.TokenPriceHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenPriceHistoryRepository extends JpaRepository<TokenPriceHistoryEntity, UUID> {

    /**
     * Find the most recent price for a token before a given timestamp.
     * Used for 24h change calculation.
     */
    @Query("SELECT p FROM TokenPriceHistoryEntity p " +
            "WHERE p.symbol = :symbol AND p.chainType = :chainType " +
            "AND p.recordedAt <= :beforeTime " +
            "ORDER BY p.recordedAt DESC LIMIT 1")
    Optional<TokenPriceHistoryEntity> findClosestPriceBefore(
            @Param("symbol") String symbol,
            @Param("chainType") String chainType,
            @Param("beforeTime") LocalDateTime beforeTime);

    /**
     * Find the latest price for a token.
     */
    @Query("SELECT p FROM TokenPriceHistoryEntity p " +
            "WHERE p.symbol = :symbol AND p.chainType = :chainType " +
            "ORDER BY p.recordedAt DESC LIMIT 1")
    Optional<TokenPriceHistoryEntity> findLatestPrice(
            @Param("symbol") String symbol,
            @Param("chainType") String chainType);

    /**
     * Get all prices for a token within a time range.
     */
    List<TokenPriceHistoryEntity> findBySymbolAndChainTypeAndRecordedAtBetweenOrderByRecordedAtDesc(
            String symbol, String chainType, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Delete old price records for cleanup.
     */
    @Modifying
    @Query("DELETE FROM TokenPriceHistoryEntity p WHERE p.recordedAt < :beforeTime")
    int deleteOlderThan(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * Get distinct token symbols and chains that have been recorded.
     */
    @Query("SELECT DISTINCT p.symbol, p.chainType FROM TokenPriceHistoryEntity p")
    List<Object[]> findDistinctTokens();
}
