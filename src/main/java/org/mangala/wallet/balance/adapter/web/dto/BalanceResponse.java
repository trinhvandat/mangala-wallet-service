package org.mangala.wallet.balance.adapter.web.dto;

import java.time.LocalDateTime;

public record BalanceResponse(
        String tokenAddress,
        String symbol,
        String name,
        Integer decimals,
        String balance,
        String balanceRaw,
        LocalDateTime lastSyncedAt
) {
}
