package org.mangala.wallet.balance.adapter.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.balance.adapter.web.dto.SyncResponse;
import org.mangala.wallet.balance.adapter.web.dto.WalletBalancesResponse;
import org.mangala.wallet.balance.usecase.GetWalletBalancesUseCase;
import org.mangala.wallet.balance.usecase.SyncWalletBalanceUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class BalanceController {

    private final GetWalletBalancesUseCase getWalletBalancesUseCase;
    private final SyncWalletBalanceUseCase syncWalletBalanceUseCase;

    @GetMapping("/{walletId}/balances")
    public ResponseEntity<WalletBalancesResponse> getBalances(@PathVariable UUID walletId) {
        log.debug("GET /api/v1/wallets/{}/balances", walletId);
        WalletBalancesResponse response = getWalletBalancesUseCase.execute(walletId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{walletId}/sync")
    public ResponseEntity<SyncResponse> syncWallet(@PathVariable UUID walletId) {
        log.info("POST /api/v1/wallets/{}/sync", walletId);
        SyncResponse response = syncWalletBalanceUseCase.execute(walletId);
        return ResponseEntity.ok(response);
    }
}
