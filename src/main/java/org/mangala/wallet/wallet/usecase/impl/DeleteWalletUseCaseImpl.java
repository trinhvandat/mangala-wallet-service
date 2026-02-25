package org.mangala.wallet.wallet.usecase.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.shared.constant.ErrorConstant;
import org.mangala.wallet.shared.exception.WalletException;
import org.mangala.wallet.wallet.adapter.repository.WalletRepository;
import org.mangala.wallet.wallet.domain.WalletEntity;
import org.mangala.wallet.wallet.usecase.DeleteWalletUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteWalletUseCaseImpl implements DeleteWalletUseCase {

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public void execute(UUID walletId, UUID userId) {
        log.debug("Deleting wallet {} for user {}", walletId, userId);

        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletException(ErrorConstant.WALLET_NOT_FOUND));

        // Verify ownership
        if (!wallet.getUserId().equals(userId)) {
            throw new WalletException(ErrorConstant.WALLET_ACCESS_DENIED);
        }

        // Soft delete
        wallet.setIsActive(false);
        walletRepository.save(wallet);

        log.info("Deleted wallet {} for user {}", walletId, userId);
    }
}
