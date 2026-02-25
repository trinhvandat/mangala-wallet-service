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
import org.mangala.wallet.wallet.usecase.GetWalletUseCase;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetWalletUseCaseImpl")
class GetWalletUseCaseImplTest {

    @Mock
    private WalletRepository walletRepository;

    private GetWalletUseCaseImpl useCase;

    private static final UUID WALLET_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ADDRESS = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e";

    @BeforeEach
    void setUp() {
        useCase = new GetWalletUseCaseImpl(walletRepository);
    }

    @Test
    @DisplayName("getById should return wallet when found")
    void getByIdSuccess() {
        LocalDateTime now = LocalDateTime.now();
        WalletEntity wallet = WalletEntity.builder()
                .id(WALLET_ID)
                .userId(USER_ID)
                .address(ADDRESS)
                .chainType(ChainType.ETHEREUM.name())
                .label("My Wallet")
                .isActive(true)
                .createdAt(now)
                .lastSyncedAt(now)
                .build();

        when(walletRepository.findById(WALLET_ID)).thenReturn(Optional.of(wallet));

        GetWalletUseCase.Response response = useCase.getById(WALLET_ID);

        assertThat(response.getId()).isEqualTo(WALLET_ID);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getAddress()).isEqualTo(ADDRESS);
        assertThat(response.getChainType()).isEqualTo(ChainType.ETHEREUM);
        assertThat(response.getLabel()).isEqualTo("My Wallet");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("getById should throw exception when not found")
    void getByIdNotFound() {
        when(walletRepository.findById(WALLET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getById(WALLET_ID))
                .isInstanceOf(WalletException.class)
                .satisfies(ex -> {
                    WalletException we = (WalletException) ex;
                    assertThat(we.getErrorDefinition()).isEqualTo(ErrorConstant.WALLET_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("getByUserId should return all active wallets for user")
    void getByUserIdSuccess() {
        LocalDateTime now = LocalDateTime.now();
        WalletEntity wallet1 = WalletEntity.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .address(ADDRESS)
                .chainType(ChainType.ETHEREUM.name())
                .label("ETH Wallet")
                .isActive(true)
                .createdAt(now)
                .build();

        WalletEntity wallet2 = WalletEntity.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .address("0xAnotherAddress1234567890123456789012345678")
                .chainType(ChainType.BSC.name())
                .label("BSC Wallet")
                .isActive(true)
                .createdAt(now)
                .build();

        when(walletRepository.findByUserIdAndIsActiveTrue(USER_ID))
                .thenReturn(List.of(wallet1, wallet2));

        List<GetWalletUseCase.Response> responses = useCase.getByUserId(USER_ID);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(GetWalletUseCase.Response::getChainType)
                .containsExactlyInAnyOrder(ChainType.ETHEREUM, ChainType.BSC);
    }

    @Test
    @DisplayName("getByUserId should return empty list when user has no wallets")
    void getByUserIdEmpty() {
        when(walletRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(List.of());

        List<GetWalletUseCase.Response> responses = useCase.getByUserId(USER_ID);

        assertThat(responses).isEmpty();
    }
}
