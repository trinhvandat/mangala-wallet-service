package org.mangala.wallet.wallet.usecase.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.shared.constant.ErrorConstant;
import org.mangala.wallet.shared.exception.WalletException;
import org.mangala.wallet.wallet.adapter.repository.WalletRepository;
import org.mangala.wallet.wallet.domain.WalletEntity;
import org.mangala.wallet.wallet.usecase.DeleteWalletUseCase;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteWalletUseCaseImpl implements DeleteWalletUseCase {

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    @CacheEvict(value = {"portfolioSummary", "portfolioSummaryByChain"}, allEntries = true)
    public void execute(UUID walletId, UUID userId) {
        log.debug("Deleting wallet {} for user {}", walletId, userId);

        Optional<WalletEntity> walletOpt = walletRepository.findById(walletId);

        // Idempotent: if wallet doesn't exist, return silently
        if (walletOpt.isEmpty()) {
            log.debug("Wallet {} not found, returning silently (idempotent delete)", walletId);
            return;
        }

        WalletEntity wallet = walletOpt.get();

        // Verify ownership
        if (!wallet.getUserId().equals(userId)) {
            throw new WalletException(ErrorConstant.WALLET_ACCESS_DENIED);
        }

        // Idempotent: if already deleted, return silently
        if (!wallet.getIsActive() || wallet.getDeletedAt() != null) {
            log.debug("Wallet {} already deleted, returning silently (idempotent delete)", walletId);
            return;
        }

        // Soft delete with timestamp
        wallet.setIsActive(false);
        wallet.setDeletedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        log.info("Deleted wallet {} for user {}", walletId, userId);
    }
}
