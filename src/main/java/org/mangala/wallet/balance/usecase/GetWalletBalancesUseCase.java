package org.mangala.wallet.balance.usecase;

import org.mangala.wallet.balance.adapter.web.dto.WalletBalancesResponse;

import java.util.UUID;

public interface GetWalletBalancesUseCase {

    WalletBalancesResponse execute(UUID walletId);
}
