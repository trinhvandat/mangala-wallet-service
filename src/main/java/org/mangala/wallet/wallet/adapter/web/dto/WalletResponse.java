package org.mangala.wallet.wallet.adapter.web.dto;

import lombok.Builder;
import lombok.Data;
import org.mangala.wallet.chain.domain.ChainType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WalletResponse {
    private UUID id;
    private UUID userId;
    private String address;
    private ChainType chainType;
    private String label;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastSyncedAt;
}
