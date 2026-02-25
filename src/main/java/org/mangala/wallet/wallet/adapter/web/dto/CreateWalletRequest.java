package org.mangala.wallet.wallet.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.mangala.wallet.chain.domain.ChainType;

@Data
public class CreateWalletRequest {

    @NotBlank(message = "Address is required")
    private String address;

    @NotNull(message = "Chain type is required")
    private ChainType chainType;

    private String label;
}
