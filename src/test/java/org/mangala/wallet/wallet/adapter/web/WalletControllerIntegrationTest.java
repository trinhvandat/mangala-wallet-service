package org.mangala.wallet.wallet.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mangala.wallet.chain.adapter.ChainAdapter;
import org.mangala.wallet.chain.adapter.ChainAdapterResolver;
import org.mangala.wallet.chain.domain.ChainType;
import org.mangala.wallet.wallet.adapter.repository.WalletRepository;
import org.mangala.wallet.wallet.adapter.web.dto.CreateWalletRequest;
import org.mangala.wallet.wallet.domain.WalletEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("WalletController Integration Tests")
class WalletControllerIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ChainAdapterResolver testChainAdapterResolver() {
            ChainAdapterResolver resolver = mock(ChainAdapterResolver.class);
            ChainAdapter adapter = mock(ChainAdapter.class);

            when(adapter.isValidAddress(any())).thenReturn(true);
            when(adapter.normalizeAddress(any())).thenAnswer(inv -> inv.getArgument(0));
            when(adapter.getChainType()).thenReturn(ChainType.ETHEREUM);

            when(resolver.getAdapter(any(ChainType.class))).thenReturn(Optional.of(adapter));

            return resolver;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String ADDRESS = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e";

    @Test
    @DisplayName("POST /api/v1/wallets - should create wallet")
    void createWallet() throws Exception {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setAddress(ADDRESS);
        request.setChainType(ChainType.ETHEREUM);
        request.setLabel("My ETH Wallet");

        mockMvc.perform(post("/api/v1/wallets")
                        .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.address", is(ADDRESS)))
                .andExpect(jsonPath("$.chainType", is("ETHEREUM")))
                .andExpect(jsonPath("$.label", is("My ETH Wallet")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/wallets - should return 400 for missing address")
    void createWalletMissingAddress() throws Exception {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setChainType(ChainType.ETHEREUM);

        mockMvc.perform(post("/api/v1/wallets")
                        .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/wallets - should return 409 for duplicate wallet")
    void createWalletDuplicate() throws Exception {
        // Create existing wallet
        WalletEntity existing = WalletEntity.builder()
                .userId(UUID.fromString(USER_ID))
                .address(ADDRESS)
                .chainType(ChainType.ETHEREUM.name())
                .label("Existing")
                .isActive(true)
                .build();
        walletRepository.save(existing);

        CreateWalletRequest request = new CreateWalletRequest();
        request.setAddress(ADDRESS);
        request.setChainType(ChainType.ETHEREUM);
        request.setLabel("Duplicate");

        mockMvc.perform(post("/api/v1/wallets")
                        .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/wallets - should return user's wallets")
    void getMyWallets() throws Exception {
        // Create test wallets
        WalletEntity wallet1 = WalletEntity.builder()
                .userId(UUID.fromString(USER_ID))
                .address(ADDRESS)
                .chainType(ChainType.ETHEREUM.name())
                .label("Wallet 1")
                .isActive(true)
                .build();
        walletRepository.save(wallet1);

        WalletEntity wallet2 = WalletEntity.builder()
                .userId(UUID.fromString(USER_ID))
                .address("0xAnotherAddress1234567890123456789012345678")
                .chainType(ChainType.BSC.name())
                .label("Wallet 2")
                .isActive(true)
                .build();
        walletRepository.save(wallet2);

        mockMvc.perform(get("/api/v1/wallets")
                        .with(jwt().jwt(builder -> builder.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].label", containsInAnyOrder("Wallet 1", "Wallet 2")));
    }

    @Test
    @DisplayName("GET /api/v1/wallets/{id} - should return wallet by id")
    void getWalletById() throws Exception {
        WalletEntity wallet = WalletEntity.builder()
                .userId(UUID.fromString(USER_ID))
                .address(ADDRESS)
                .chainType(ChainType.ETHEREUM.name())
                .label("Test Wallet")
                .isActive(true)
                .build();
        wallet = walletRepository.save(wallet);

        mockMvc.perform(get("/api/v1/wallets/{id}", wallet.getId())
                        .with(jwt().jwt(builder -> builder.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(wallet.getId().toString())))
                .andExpect(jsonPath("$.label", is("Test Wallet")));
    }

    @Test
    @DisplayName("GET /api/v1/wallets/{id} - should return 404 for non-existent wallet")
    void getWalletByIdNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/wallets/{id}", nonExistentId)
                        .with(jwt().jwt(builder -> builder.subject(USER_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/wallets/{id} - should soft delete wallet")
    void deleteWallet() throws Exception {
        WalletEntity wallet = WalletEntity.builder()
                .userId(UUID.fromString(USER_ID))
                .address(ADDRESS)
                .chainType(ChainType.ETHEREUM.name())
                .label("To Delete")
                .isActive(true)
                .build();
        wallet = walletRepository.save(wallet);

        mockMvc.perform(delete("/api/v1/wallets/{id}", wallet.getId())
                        .with(jwt().jwt(builder -> builder.subject(USER_ID))))
                .andExpect(status().isNoContent());

        // Verify soft delete
        WalletEntity deleted = walletRepository.findById(wallet.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(deleted.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("DELETE /api/v1/wallets/{id} - should return 403 for other user's wallet")
    void deleteWalletAccessDenied() throws Exception {
        String otherUserId = "660e8400-e29b-41d4-a716-446655440001";
        WalletEntity wallet = WalletEntity.builder()
                .userId(UUID.fromString(otherUserId))
                .address(ADDRESS)
                .chainType(ChainType.ETHEREUM.name())
                .label("Other's Wallet")
                .isActive(true)
                .build();
        wallet = walletRepository.save(wallet);

        mockMvc.perform(delete("/api/v1/wallets/{id}", wallet.getId())
                        .with(jwt().jwt(builder -> builder.subject(USER_ID))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 for unauthenticated request")
    void unauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/wallets"))
                .andExpect(status().isUnauthorized());
    }
}
