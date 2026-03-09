package org.mangala.wallet.portfolio.adapter.repository;

import org.mangala.wallet.portfolio.domain.UserPortfolioBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPortfolioBalanceRepository extends JpaRepository<UserPortfolioBalanceEntity, UUID> {

    List<UserPortfolioBalanceEntity> findByUserId(UUID userId);

    List<UserPortfolioBalanceEntity> findByUserIdAndChainType(UUID userId, String chainType);

    @Query("SELECT p FROM UserPortfolioBalanceEntity p WHERE p.userId = :userId AND p.totalBalance > 0")
    List<UserPortfolioBalanceEntity> findNonZeroBalancesByUserId(@Param("userId") UUID userId);

    @Query("SELECT p FROM UserPortfolioBalanceEntity p WHERE p.userId = :userId " +
           "AND p.chainType = :chainType AND (p.contractAddress = :contractAddress OR " +
           "(p.contractAddress IS NULL AND :contractAddress IS NULL))")
    Optional<UserPortfolioBalanceEntity> findByUserIdAndChainTypeAndContractAddress(
            @Param("userId") UUID userId,
            @Param("chainType") String chainType,
            @Param("contractAddress") String contractAddress);

    @Query("SELECT p FROM UserPortfolioBalanceEntity p WHERE p.userId = :userId " +
           "AND p.chainType = :chainType AND p.contractAddress IS NULL")
    Optional<UserPortfolioBalanceEntity> findNativeTokenBalance(
            @Param("userId") UUID userId,
            @Param("chainType") String chainType);

    @Query("SELECT DISTINCT p.chainType FROM UserPortfolioBalanceEntity p WHERE p.userId = :userId")
    List<String> findDistinctChainTypesByUserId(@Param("userId") UUID userId);

    void deleteByUserId(UUID userId);
}
