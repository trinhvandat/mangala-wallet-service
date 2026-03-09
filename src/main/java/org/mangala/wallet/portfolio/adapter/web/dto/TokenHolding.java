package org.mangala.wallet.portfolio.adapter.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Individual token holding information")
public class TokenHolding {

    @Schema(description = "Token symbol", example = "ETH")
    private final String symbol;

    @Schema(description = "Token name", example = "Ethereum")
    private final String name;

    @Schema(description = "Blockchain where token resides", example = "ETHEREUM")
    private final String chain;

    @Schema(description = "Contract address (null for native tokens)")
    private final String contractAddress;

    @Schema(description = "Total quantity held across all wallets", example = "2.5")
    private final BigDecimal quantity;

    @Schema(description = "Current price per token in USD", example = "3500.00")
    private final BigDecimal priceUsd;

    @Schema(description = "Total value in USD (quantity * price)", example = "8750.00")
    private final BigDecimal valueUsd;

    @Schema(description = "24-hour price change percentage", example = "2.1")
    private final BigDecimal change24hPercent;

    @Schema(description = "Percentage of total portfolio value", example = "57.4")
    private final BigDecimal allocationPercent;

    @Schema(description = "Number of wallets holding this token", example = "3")
    private final Integer walletCount;
}
