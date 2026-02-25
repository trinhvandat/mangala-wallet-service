package org.mangala.wallet.wallet.usecase;

import java.util.UUID;

public interface DeleteWalletUseCase {

    void execute(UUID walletId, UUID userId);
}
