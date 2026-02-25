package org.mangala.wallet.wallet.usecase;

import lombok.Builder;
import lombok.Data;
import org.mangala.wallet.chain.domain.ChainType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface GetWalletUseCase {

    Response getById(UUID walletId);

    List<Response> getByUserId(UUID userId);

    @Data
    @Builder
    class Response {
        private final UUID id;
        private final UUID userId;
        private final String address;
        private final ChainType chainType;
        private final String label;
        private final Boolean isActive;
        private final LocalDateTime createdAt;
        private final LocalDateTime lastSyncedAt;
    }
}
