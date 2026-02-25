package org.mangala.wallet.wallet.adapter.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mangala.wallet.wallet.adapter.web.dto.CreateWalletRequest;
import org.mangala.wallet.wallet.adapter.web.dto.WalletResponse;
import org.mangala.wallet.wallet.usecase.CreateWalletUseCase;
import org.mangala.wallet.wallet.usecase.DeleteWalletUseCase;
import org.mangala.wallet.wallet.usecase.GetWalletUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final CreateWalletUseCase createWalletUseCase;
    private final GetWalletUseCase getWalletUseCase;
    private final DeleteWalletUseCase deleteWalletUseCase;

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateWalletRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());

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
    public ResponseEntity<List<WalletResponse>> getMyWallets(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        List<GetWalletUseCase.Response> wallets = getWalletUseCase.getByUserId(userId);

        List<WalletResponse> responses = wallets.stream()
                .map(this::mapToWalletResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable UUID walletId) {
        GetWalletUseCase.Response wallet = getWalletUseCase.getById(walletId);
        return ResponseEntity.ok(mapToWalletResponse(wallet));
    }

    @DeleteMapping("/{walletId}")
    public ResponseEntity<Void> deleteWallet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID walletId) {

        UUID userId = UUID.fromString(jwt.getSubject());
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
