package org.mangala.wallet.wallet.adapter.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mangala.security.preauthenticated.PreAuthenticatedPrincipal;
import org.mangala.wallet.chain.domain.ChainType;
import org.mangala.wallet.shared.dto.ErrorResponse;
import org.mangala.wallet.wallet.adapter.web.dto.CreateWalletRequest;
import org.mangala.wallet.wallet.adapter.web.dto.WalletPageResponse;
import org.mangala.wallet.wallet.adapter.web.dto.WalletResponse;
import org.mangala.wallet.wallet.usecase.CreateWalletUseCase;
import org.mangala.wallet.wallet.usecase.DeleteWalletUseCase;
import org.mangala.wallet.wallet.usecase.GetWalletUseCase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "Wallet management endpoints")
public class WalletController {

    private final CreateWalletUseCase createWalletUseCase;
    private final GetWalletUseCase getWalletUseCase;
    private final DeleteWalletUseCase deleteWalletUseCase;

    @PostMapping
    @Operation(
            summary = "Create a new wallet",
            description = "Register a new blockchain wallet for the authenticated user. " +
                    "If a wallet with the same address and chain already exists, returns the existing wallet (idempotent).")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Wallet created successfully",
                    content = @Content(schema = @Schema(implementation = WalletResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (validation error or invalid address format)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 400,
                                      "errorCode": "WAL-004",
                                      "message": "Invalid wallet address format",
                                      "timestamp": "2024-01-15T10:30:00Z",
                                      "path": "/api/v1/wallets"
                                    }
                                    """))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing authentication token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<WalletResponse> createWallet(
            @AuthenticationPrincipal PreAuthenticatedPrincipal principal,
            @Valid @RequestBody CreateWalletRequest request) {

        UUID userId = UUID.fromString(principal.getUserId());

        CreateWalletUseCase.Command command = CreateWalletUseCase.Command.builder()
                .userId(userId)
                .address(request.getAddress())
                .chainType(request.getChainType())
                .label(request.getLabel())
                .build();

        CreateWalletUseCase.Response result = createWalletUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToWalletResponse(result));
    }

    @GetMapping
    @Operation(
            summary = "List user wallets",
            description = "Retrieve a paginated list of wallets belonging to the authenticated user. " +
                    "Results can be filtered by chain type and are sorted by creation date (newest first).")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wallets retrieved successfully",
                    content = @Content(schema = @Schema(implementation = WalletPageResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing authentication token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<WalletPageResponse> getMyWallets(
            @AuthenticationPrincipal PreAuthenticatedPrincipal principal,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by blockchain network type")
            @RequestParam(required = false) ChainType chain
    ) {
        UUID userId = UUID.fromString(principal.getUserId());

        // Cap size at 100 to prevent excessive queries
        int cappedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, cappedSize);

        GetWalletUseCase.PagedResponse pagedResult = getWalletUseCase.getByUserIdPaginated(userId, chain, pageable);

        List<WalletResponse> walletResponses = pagedResult.getWallets().stream()
                .map(this::mapToWalletResponse)
                .toList();

        WalletPageResponse response = WalletPageResponse.builder()
                .wallets(walletResponses)
                .page(pagedResult.getPage())
                .size(pagedResult.getSize())
                .totalElements(pagedResult.getTotalElements())
                .totalPages(pagedResult.getTotalPages())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{walletId}")
    @Operation(
            summary = "Get wallet by ID",
            description = "Retrieve details of a specific wallet by its unique identifier.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wallet found",
                    content = @Content(schema = @Schema(implementation = WalletResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 404,
                                      "errorCode": "WAL-001",
                                      "message": "Wallet not found",
                                      "timestamp": "2024-01-15T10:30:00Z",
                                      "path": "/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000"
                                    }
                                    """))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing authentication token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<WalletResponse> getWallet(
            @Parameter(description = "Wallet UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID walletId) {
        GetWalletUseCase.Response wallet = getWalletUseCase.getById(walletId);
        return ResponseEntity.ok(mapToWalletResponse(wallet));
    }

    @DeleteMapping("/{walletId}")
    @Operation(
            summary = "Delete a wallet",
            description = "Soft-delete a wallet belonging to the authenticated user. " +
                    "The wallet will be marked as inactive and excluded from future queries.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Wallet deleted successfully"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - wallet belongs to another user",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 403,
                                      "errorCode": "WAL-003",
                                      "message": "Access denied to this wallet",
                                      "timestamp": "2024-01-15T10:30:00Z",
                                      "path": "/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000"
                                    }
                                    """))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing authentication token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteWallet(
            @AuthenticationPrincipal PreAuthenticatedPrincipal principal,
            @Parameter(description = "Wallet UUID to delete", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID walletId) {

        UUID userId = UUID.fromString(principal.getUserId());
        deleteWalletUseCase.execute(walletId, userId);

        return ResponseEntity.noContent().build();
    }

    private WalletResponse mapToWalletResponse(CreateWalletUseCase.Response result) {
        return WalletResponse.builder()
                .id(result.getId())
                .userId(result.getUserId())
                .address(result.getAddress())
                .chainType(result.getChainType())
                .label(result.getLabel())
                .createdAt(result.getCreatedAt())
                .build();
    }

    private WalletResponse mapToWalletResponse(GetWalletUseCase.Response result) {
        return WalletResponse.builder()
                .id(result.getId())
                .userId(result.getUserId())
                .address(result.getAddress())
                .chainType(result.getChainType())
                .label(result.getLabel())
                .isActive(result.getIsActive())
                .createdAt(result.getCreatedAt())
                .lastSyncedAt(result.getLastSyncedAt())
                .build();
    }
}
