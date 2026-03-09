package org.mangala.wallet.wallet.usecase;

import lombok.Builder;
import lombok.Data;
import org.mangala.wallet.chain.domain.ChainType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface GetWalletUseCase {

    Response getById(UUID walletId);

    List<Response> getByUserId(UUID userId);

    /**
     * Get paginated wallets for a user with optional chain filter.
     * @param userId The user ID
     * @param chainType Optional chain filter (null for all chains)
     * @param pageable Pagination parameters
     * @return Page of wallet responses
     */
    PagedResponse getByUserIdPaginated(UUID userId, ChainType chainType, Pageable pageable);

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

    @Data
    @Builder
    class PagedResponse {
        private final List<Response> wallets;
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
    }
}
