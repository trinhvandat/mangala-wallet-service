package org.mangala.wallet.balance.adapter.web.dto;

import java.util.UUID;

public record SyncResponse(
        UUID walletId,
        String status,
        Integer tokensFound,
        Long syncDurationMs,
        String message
) {
}
