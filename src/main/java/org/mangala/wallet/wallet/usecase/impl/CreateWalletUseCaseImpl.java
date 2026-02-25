package org.mangala.wallet.wallet.usecase.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.chain.adapter.ChainAdapter;
import org.mangala.wallet.chain.adapter.ChainAdapterResolver;
import org.mangala.wallet.shared.constant.ErrorConstant;
import org.mangala.wallet.shared.exception.WalletException;
import org.mangala.wallet.wallet.adapter.repository.WalletRepository;
import org.mangala.wallet.wallet.domain.WalletEntity;
import org.mangala.wallet.wallet.usecase.CreateWalletUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateWalletUseCaseImpl implements CreateWalletUseCase {

    private final WalletRepository walletRepository;
    private final ChainAdapterResolver chainAdapterResolver;

    @Override
    @Transactional
    public Response execute(Command command) {
        log.debug("Creating wallet for user {} on chain {}", command.getUserId(), command.getChainType());

        // Get chain adapter and validate address
        ChainAdapter adapter = chainAdapterResolver.getAdapter(command.getChainType())
                .orElseThrow(() -> new WalletException(ErrorConstant.CHAIN_NOT_SUPPORTED));

        if (!adapter.isValidAddress(command.getAddress())) {
            throw new WalletException(ErrorConstant.INVALID_WALLET_ADDRESS);
        }

        // Normalize address
        String normalizedAddress = adapter.normalizeAddress(command.getAddress());

        // Check for duplicate
        boolean exists = walletRepository.existsByUserIdAndAddressAndChainTypeAndIsActiveTrue(
                command.getUserId(),
                normalizedAddress,
                command.getChainType().name()
        );

        if (exists) {
            throw new WalletException(ErrorConstant.WALLET_ALREADY_EXISTS);
        }

        // Create wallet entity
        WalletEntity wallet = WalletEntity.builder()
                .userId(command.getUserId())
                .address(normalizedAddress)
                .chainType(command.getChainType().name())
                .label(command.getLabel())
                .isActive(true)
                .build();

        WalletEntity saved = walletRepository.save(wallet);

        log.info("Created wallet {} for user {} on chain {}", saved.getId(), command.getUserId(), command.getChainType());

        return Response.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .address(saved.getAddress())
                .chainType(command.getChainType())
                .label(saved.getLabel())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
