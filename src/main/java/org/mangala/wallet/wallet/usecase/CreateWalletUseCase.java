package org.mangala.wallet.wallet.usecase;

import lombok.Builder;
import lombok.Data;
import org.mangala.wallet.chain.domain.ChainType;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CreateWalletUseCase {

    Response execute(Command command);

    @Data
    @Builder
    class Command {
        private final UUID userId;
        private final String address;
        private final ChainType chainType;
        private final String label;
    }

    @Data
    @Builder
    class Response {
        private final UUID id;
        private final UUID userId;
        private final String address;
        private final ChainType chainType;
        private final String label;
        private final LocalDateTime createdAt;
    }
}
