package org.mangala.wallet.wallet.adapter.repository;

import org.mangala.wallet.wallet.domain.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {

    List<WalletEntity> findByUserIdAndIsActiveTrue(UUID userId);

    Optional<WalletEntity> findByUserIdAndAddressAndChainTypeAndIsActiveTrue(UUID userId, String address, String chainType);

    boolean existsByUserIdAndAddressAndChainTypeAndIsActiveTrue(UUID userId, String address, String chainType);

    List<WalletEntity> findAllByIsActiveTrue();
}
