package org.mangala.wallet.portfolio.adapter.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.portfolio.adapter.web.dto.PortfolioSummaryResponse;
import org.mangala.wallet.portfolio.service.PortfolioAggregationService;
import org.mangala.wallet.portfolio.usecase.GetPortfolioSummaryUseCase;
import org.mangala.security.preauthenticated.PreAuthenticatedPrincipal;
import org.mangala.wallet.shared.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Portfolio management and summary endpoints")
public class PortfolioController {

    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final PortfolioAggregationService portfolioAggregationService;

    @GetMapping("/summary")
    @Operation(
            summary = "Get portfolio summary",
            description = "Returns a comprehensive portfolio summary including total value, " +
                    "token breakdown with allocations, and chain-level summaries. " +
                    "Optionally filter by chain type using the 'chain' query parameter."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Portfolio summary retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PortfolioSummaryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - valid authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<PortfolioSummaryResponse> getPortfolioSummary(
            @AuthenticationPrincipal PreAuthenticatedPrincipal principal,
            @Parameter(description = "Optional chain type filter", example = "ETHEREUM")
            @RequestParam(required = false) String chain) {

        UUID userId = UUID.fromString(principal.getUserId());
        log.info("Getting portfolio summary for user: {} chain: {}", userId, chain);

        PortfolioSummaryResponse summary;
        if (chain != null && !chain.isBlank()) {
            summary = getPortfolioSummaryUseCase.getPortfolioSummaryByChain(userId, chain);
        } else {
            summary = getPortfolioSummaryUseCase.getPortfolioSummary(userId);
        }

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/summary/chain/{chainType}")
    @Operation(
            summary = "Get portfolio summary by chain",
            description = "Returns portfolio summary filtered by specific blockchain"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Chain-filtered portfolio summary retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PortfolioSummaryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid chain type",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<PortfolioSummaryResponse> getPortfolioSummaryByChain(
            @AuthenticationPrincipal PreAuthenticatedPrincipal principal,
            @Parameter(description = "Chain type to filter by", example = "ETHEREUM")
            @PathVariable String chainType) {

        UUID userId = UUID.fromString(principal.getUserId());
        log.info("Getting portfolio summary for user: {} chain: {}", userId, chainType);

        PortfolioSummaryResponse summary = getPortfolioSummaryUseCase
                .getPortfolioSummaryByChain(userId, chainType);

        return ResponseEntity.ok(summary);
    }

    @PostMapping("/recalculate")
    @Operation(
            summary = "Recalculate portfolio",
            description = "Triggers a full recalculation of the user's portfolio from wallet balances. " +
                    "Useful after manual data corrections or initial setup."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Portfolio recalculation triggered successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Void> recalculatePortfolio(
            @AuthenticationPrincipal PreAuthenticatedPrincipal principal) {

        UUID userId = UUID.fromString(principal.getUserId());
        log.info("Triggering portfolio recalculation for user: {}", userId);

        portfolioAggregationService.recalculateUserPortfolio(userId);

        return ResponseEntity.noContent().build();
    }
}
