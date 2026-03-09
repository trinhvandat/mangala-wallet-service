package org.mangala.wallet.wallet.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.mangala.wallet.chain.domain.ChainType;

@Data
@Schema(description = "Request to create a new wallet")
public class CreateWalletRequest {

    @NotBlank(message = "Address is required")
    @Schema(
            description = "Blockchain wallet address",
            example = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String address;

    @NotNull(message = "Chain type is required")
    @Schema(
            description = "Blockchain network type",
            example = "ETHEREUM",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private ChainType chainType;

    @Size(max = 50, message = "Label must be at most 50 characters")
    @Schema(
            description = "Optional user-friendly label for the wallet",
            example = "My Main Wallet",
            maxLength = 50)
    private String label;
}
