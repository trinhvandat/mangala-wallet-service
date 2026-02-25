package org.mangala.wallet.wallet.usecase.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.chain.domain.ChainType;
import org.mangala.wallet.shared.constant.ErrorConstant;
import org.mangala.wallet.shared.exception.WalletException;
import org.mangala.wallet.wallet.adapter.repository.WalletRepository;
import org.mangala.wallet.wallet.domain.WalletEntity;
import org.mangala.wallet.wallet.usecase.GetWalletUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetWalletUseCaseImpl implements GetWalletUseCase {

    private final WalletRepository walletRepository;

    @Override
    @Transactional(readOnly = true)
    public Response getById(UUID walletId) {
        log.debug("Getting wallet by id: {}", walletId);

        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletException(ErrorConstant.WALLET_NOT_FOUND));

        return mapToResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Response> getByUserId(UUID userId) {
        log.debug("Getting wallets for user: {}", userId);

        List<WalletEntity> wallets = walletRepository.findByUserIdAndIsActiveTrue(userId);

        return wallets.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Response mapToResponse(WalletEntity wallet) {
        return Response.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .address(wallet.getAddress())
                .chainType(ChainType.valueOf(wallet.getChainType()))
                .label(wallet.getLabel())
                .isActive(wallet.getIsActive())
                .createdAt(wallet.getCreatedAt())
                .lastSyncedAt(wallet.getLastSyncedAt())
                .build();
    }
}
