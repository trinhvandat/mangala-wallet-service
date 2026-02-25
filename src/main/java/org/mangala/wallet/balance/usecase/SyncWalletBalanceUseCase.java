package org.mangala.wallet.balance.usecase;

import org.mangala.wallet.balance.adapter.web.dto.SyncResponse;

import java.util.UUID;

public interface SyncWalletBalanceUseCase {

    SyncResponse execute(UUID walletId);
}
