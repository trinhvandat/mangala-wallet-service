package org.mangala.wallet.portfolio.adapter.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Complete portfolio summary including total value, token breakdown, and chain summary")
public class PortfolioSummaryResponse {

    @Schema(description = "Total portfolio value in USD", example = "15234.56")
    private final BigDecimal totalValueUsd;

    @Schema(description = "24-hour change metrics")
    private final Change24h change24h;

    @Schema(description = "List of token holdings with values")
    private final List<TokenHolding> tokens;

    @Schema(description = "Summary of value per blockchain", example = "{\"ETHEREUM\": 10000.00, \"BSC\": 3000.00}")
    private final Map<String, BigDecimal> chainSummary;

    @Schema(description = "Timestamp of last portfolio update", example = "2026-03-06T10:30:00Z")
    private final Instant lastUpdatedAt;

    @Getter
    @Builder
    @Schema(description = "24-hour change information")
    public static class Change24h {
        @Schema(description = "Absolute change in USD", example = "523.12")
        private final BigDecimal amountUsd;

        @Schema(description = "Percentage change", example = "3.56")
        private final BigDecimal percentage;
    }
}
