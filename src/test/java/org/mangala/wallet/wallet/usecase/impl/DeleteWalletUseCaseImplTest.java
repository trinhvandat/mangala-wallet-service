package org.mangala.wallet.wallet.usecase.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mangala.wallet.chain.domain.ChainType;
import org.mangala.wallet.shared.constant.ErrorConstant;
import org.mangala.wallet.shared.exception.WalletException;
import org.mangala.wallet.wallet.adapter.repository.WalletRepository;
import org.mangala.wallet.wallet.domain.WalletEntity;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteWalletUseCaseImpl")
class DeleteWalletUseCaseImplTest {

    @Mock
    private WalletRepository walletRepository;

    private DeleteWalletUseCaseImpl useCase;

    private static final UUID WALLET_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new DeleteWalletUseCaseImpl(walletRepository);
    }

    @Test
    @DisplayName("should soft delete wallet successfully")
    void deleteWalletSuccess() {
        WalletEntity wallet = WalletEntity.builder()
                .id(WALLET_ID)
                .userId(USER_ID)
                .address("0x742d35Cc6634C0532925a3b844Bc454e4438f44e")
                .chainType(ChainType.ETHEREUM.name())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(walletRepository.findById(WALLET_ID)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(WalletEntity.class))).thenReturn(wallet);

        useCase.execute(WALLET_ID, USER_ID);

        ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
        verify(walletRepository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isFalse();
    }

    @Test
    @DisplayName("should throw exception when wallet not found")
    void walletNotFound() {
        when(walletRepository.findById(WALLET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(WALLET_ID, USER_ID))
                .isInstanceOf(WalletException.class)
                .satisfies(ex -> {
                    WalletException we = (WalletException) ex;
                    assertThat(we.getErrorDefinition()).isEqualTo(ErrorConstant.WALLET_NOT_FOUND);
                });

        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw exception when user does not own wallet")
    void accessDenied() {
        WalletEntity wallet = WalletEntity.builder()
                .id(WALLET_ID)
                .userId(OTHER_USER_ID) // Different user
                .address("0x742d35Cc6634C0532925a3b844Bc454e4438f44e")
                .chainType(ChainType.ETHEREUM.name())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(walletRepository.findById(WALLET_ID)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> useCase.execute(WALLET_ID, USER_ID))
                .isInstanceOf(WalletException.class)
                .satisfies(ex -> {
                    WalletException we = (WalletException) ex;
                    assertThat(we.getErrorDefinition()).isEqualTo(ErrorConstant.WALLET_ACCESS_DENIED);
                });

        verify(walletRepository, never()).save(any());
    }
}
