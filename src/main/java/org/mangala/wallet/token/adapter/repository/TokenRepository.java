package org.mangala.wallet.token.adapter.repository;

import org.mangala.wallet.token.domain.TokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<TokenEntity, UUID> {

    Optional<TokenEntity> findByChainTypeAndContractAddress(String chainType, String contractAddress);
}
