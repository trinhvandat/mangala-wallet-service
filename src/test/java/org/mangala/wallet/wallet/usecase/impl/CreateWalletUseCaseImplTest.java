package org.mangala.wallet.wallet.usecase.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mangala.wallet.chain.adapter.ChainAdapter;
import org.mangala.wallet.chain.adapter.ChainAdapterResolver;
import org.mangala.wallet.chain.domain.ChainType;
import org.mangala.wallet.shared.constant.ErrorConstant;
import org.mangala.wallet.shared.exception.WalletException;
import org.mangala.wallet.wallet.adapter.repository.WalletRepository;
import org.mangala.wallet.wallet.domain.WalletEntity;
import org.mangala.wallet.wallet.usecase.CreateWalletUseCase;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateWalletUseCaseImpl")
class CreateWalletUseCaseImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private ChainAdapterResolver chainAdapterResolver;

    @Mock
    private ChainAdapter chainAdapter;

    private CreateWalletUseCaseImpl useCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ADDRESS = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e";
    private static final String NORMALIZED_ADDRESS = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e";
    private static final ChainType CHAIN_TYPE = ChainType.ETHEREUM;
    private static final String LABEL = "My Wallet";

    @BeforeEach
    void setUp() {
        useCase = new CreateWalletUseCaseImpl(walletRepository, chainAdapterResolver);
    }

    @Test
    @DisplayName("should create wallet successfully")
    void createWalletSuccess() {
        // Given
        CreateWalletUseCase.Command command = CreateWalletUseCase.Command.builder()
                .userId(USER_ID)
                .address(ADDRESS)
                .chainType(CHAIN_TYPE)
                .label(LABEL)
                .build();

        when(chainAdapterResolver.getAdapter(CHAIN_TYPE)).thenReturn(Optional.of(chainAdapter));
        when(chainAdapter.isValidAddress(ADDRESS)).thenReturn(true);
        when(chainAdapter.normalizeAddress(ADDRESS)).thenReturn(NORMALIZED_ADDRESS);
        when(walletRepository.findByUserIdAndAddressAndChainTypeAndIsActiveTrue(
                USER_ID, NORMALIZED_ADDRESS, CHAIN_TYPE.name())).thenReturn(Optional.empty());

        UUID walletId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        WalletEntity savedWallet = WalletEntity.builder()
                .id(walletId)
                .userId(USER_ID)
                .address(NORMALIZED_ADDRESS)
                .chainType(CHAIN_TYPE.name())
                .label(LABEL)
                .isActive(true)
                .createdAt(now)
                .build();
        when(walletRepository.save(any(WalletEntity.class))).thenReturn(savedWallet);

        // When
        CreateWalletUseCase.Response response = useCase.execute(command);

        // Then
        assertThat(response.getId()).isEqualTo(walletId);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getAddress()).isEqualTo(NORMALIZED_ADDRESS);
        assertThat(response.getChainType()).isEqualTo(CHAIN_TYPE);
        assertThat(response.getLabel()).isEqualTo(LABEL);

        ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
        verify(walletRepository).save(captor.capture());
        WalletEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getAddress()).isEqualTo(NORMALIZED_ADDRESS);
        assertThat(saved.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("should throw exception when chain is not supported")
    void chainNotSupported() {
        CreateWalletUseCase.Command command = CreateWalletUseCase.Command.builder()
                .userId(USER_ID)
                .address(ADDRESS)
                .chainType(CHAIN_TYPE)
                .label(LABEL)
                .build();

        when(chainAdapterResolver.getAdapter(CHAIN_TYPE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(WalletException.class)
                .satisfies(ex -> {
                    WalletException we = (WalletException) ex;
                    assertThat(we.getErrorDefinition()).isEqualTo(ErrorConstant.CHAIN_NOT_SUPPORTED);
                });
    }

    @Test
    @DisplayName("should throw exception when address is invalid")
    void invalidAddress() {
        CreateWalletUseCase.Command command = CreateWalletUseCase.Command.builder()
                .userId(USER_ID)
                .address("invalid-address")
                .chainType(CHAIN_TYPE)
                .label(LABEL)
                .build();

        when(chainAdapterResolver.getAdapter(CHAIN_TYPE)).thenReturn(Optional.of(chainAdapter));
        when(chainAdapter.isValidAddress("invalid-address")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(WalletException.class)
                .satisfies(ex -> {
                    WalletException we = (WalletException) ex;
                    assertThat(we.getErrorDefinition()).isEqualTo(ErrorConstant.INVALID_WALLET_ADDRESS);
                });
    }

    @Test
    @DisplayName("should return existing wallet when duplicate (idempotent behavior)")
    void walletAlreadyExistsReturnsExisting() {
        // Given
        CreateWalletUseCase.Command command = CreateWalletUseCase.Command.builder()
                .userId(USER_ID)
                .address(ADDRESS)
                .chainType(CHAIN_TYPE)
                .label(LABEL)
                .build();

        UUID existingWalletId = UUID.randomUUID();
        LocalDateTime existingCreatedAt = LocalDateTime.now().minusDays(1);
        WalletEntity existingWallet = WalletEntity.builder()
                .id(existingWalletId)
                .userId(USER_ID)
                .address(NORMALIZED_ADDRESS)
                .chainType(CHAIN_TYPE.name())
                .label("Original Label")
                .isActive(true)
                .createdAt(existingCreatedAt)
                .build();

        when(chainAdapterResolver.getAdapter(CHAIN_TYPE)).thenReturn(Optional.of(chainAdapter));
        when(chainAdapter.isValidAddress(ADDRESS)).thenReturn(true);
        when(chainAdapter.normalizeAddress(ADDRESS)).thenReturn(NORMALIZED_ADDRESS);
        when(walletRepository.findByUserIdAndAddressAndChainTypeAndIsActiveTrue(
                USER_ID, NORMALIZED_ADDRESS, CHAIN_TYPE.name())).thenReturn(Optional.of(existingWallet));

        // When
        CreateWalletUseCase.Response response = useCase.execute(command);

        // Then - should return existing wallet, not create new one
        assertThat(response.getId()).isEqualTo(existingWalletId);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getAddress()).isEqualTo(NORMALIZED_ADDRESS);
        assertThat(response.getChainType()).isEqualTo(CHAIN_TYPE);
        assertThat(response.getLabel()).isEqualTo("Original Label"); // Original label preserved
        assertThat(response.getCreatedAt()).isEqualTo(existingCreatedAt);

        // Verify no save was called
        verify(walletRepository, never()).save(any());
    }
}
