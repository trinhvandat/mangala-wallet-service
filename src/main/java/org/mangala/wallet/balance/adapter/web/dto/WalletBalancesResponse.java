package org.mangala.wallet.balance.adapter.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record WalletBalancesResponse(
        UUID walletId,
        String chain,
        String address,
        List<BalanceResponse> balances,
        LocalDateTime lastSyncedAt,
        int totalTokens
) {
}
