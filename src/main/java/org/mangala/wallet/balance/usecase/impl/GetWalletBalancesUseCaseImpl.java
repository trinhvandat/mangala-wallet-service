package org.mangala.wallet.balance.usecase.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.balance.adapter.repository.WalletBalanceRepository;
import org.mangala.wallet.balance.adapter.web.dto.BalanceResponse;
import org.mangala.wallet.balance.adapter.web.dto.WalletBalancesResponse;
import org.mangala.wallet.balance.domain.WalletBalanceEntity;
import org.mangala.wallet.balance.usecase.GetWalletBalancesUseCase;
import org.mangala.wallet.shared.constant.ErrorConstant;
import org.mangala.wallet.shared.exception.WalletException;
import org.mangala.wallet.wallet.adapter.repository.WalletRepository;
import org.mangala.wallet.wallet.domain.WalletEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetWalletBalancesUseCaseImpl implements GetWalletBalancesUseCase {

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;

    @Override
    @Transactional(readOnly = true)
    public WalletBalancesResponse execute(UUID walletId) {
        log.debug("Fetching balances for walletId={}", walletId);

        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletException(ErrorConstant.WALLET_NOT_FOUND));

        List<WalletBalanceEntity> balanceEntities = walletBalanceRepository.findByWalletId(walletId);

        List<BalanceResponse> balances = balanceEntities.stream()
                .sorted(Comparator.comparing(WalletBalanceEntity::isNativeToken).reversed())
                .map(this::mapToBalanceResponse)
                .toList();

        LocalDateTime lastSyncedAt = balanceEntities.stream()
                .map(WalletBalanceEntity::getLastSyncedAt)
                .max(Comparator.naturalOrder())
                .orElse(wallet.getLastSyncedAt());

        log.debug("Found {} balances for walletId={}", balances.size(), walletId);

        return new WalletBalancesResponse(
                wallet.getId(),
                wallet.getChainType(),
                wallet.getAddress(),
                balances,
                lastSyncedAt,
                balances.size()
        );
    }

    private BalanceResponse mapToBalanceResponse(WalletBalanceEntity entity) {
        return new BalanceResponse(
                entity.isNativeToken() ? "native" : entity.getContractAddress(),
                entity.getSymbol(),
                entity.getName(),
                entity.getDecimals(),
                entity.getBalance().toPlainString(),
                entity.getBalanceRaw(),
                entity.getLastSyncedAt()
        );
    }
}
