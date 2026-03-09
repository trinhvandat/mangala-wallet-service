package org.mangala.wallet.wallet.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.mangala.wallet.chain.domain.ChainType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Wallet information response")
public class WalletResponse {

    @Schema(description = "Unique wallet identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Owner user identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "Blockchain wallet address", example = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e")
    private String address;

    @Schema(description = "Blockchain network type", example = "ETHEREUM")
    private ChainType chainType;

    @Schema(description = "User-friendly wallet label", example = "My Main Wallet")
    private String label;

    @Schema(description = "Whether the wallet is currently active", example = "true")
    private Boolean isActive;

    @Schema(description = "Timestamp when the wallet was created", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of last balance synchronization", example = "2024-01-15T12:00:00")
    private LocalDateTime lastSyncedAt;
}
