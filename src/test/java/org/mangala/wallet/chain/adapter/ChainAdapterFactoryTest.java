package org.mangala.wallet.chain.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mangala.wallet.chain.domain.Balance;
import org.mangala.wallet.chain.domain.ChainType;
import org.mangala.wallet.chain.domain.TokenBalance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChainAdapterFactory")
class ChainAdapterFactoryTest {

    private ChainAdapterFactory factory;
    private TestChainAdapter ethereumAdapter;
    private TestChainAdapter bscAdapter;

    @BeforeEach
    void setUp() {
        ethereumAdapter = new TestChainAdapter(ChainType.ETHEREUM);
        bscAdapter = new TestChainAdapter(ChainType.BSC);
        factory = new ChainAdapterFactory(List.of(ethereumAdapter, bscAdapter));
    }

    @Test
    @DisplayName("getAdapter should return adapter for supported chain")
    void getAdapterForSupportedChain() {
        Optional<ChainAdapter> adapter = factory.getAdapter(ChainType.ETHEREUM);

        assertThat(adapter).isPresent();
        assertThat(adapter.get().getChainType()).isEqualTo(ChainType.ETHEREUM);
    }

    @Test
    @DisplayName("getAdapter should return empty for unsupported chain")
    void getAdapterForUnsupportedChain() {
        Optional<ChainAdapter> adapter = factory.getAdapter(ChainType.POLYGON);

        assertThat(adapter).isEmpty();
    }

    @Test
    @DisplayName("getAdapterOrThrow should return adapter for supported chain")
    void getAdapterOrThrowForSupportedChain() {
        ChainAdapter adapter = factory.getAdapterOrThrow(ChainType.BSC);

        assertThat(adapter.getChainType()).isEqualTo(ChainType.BSC);
    }

    @Test
    @DisplayName("getAdapterOrThrow should throw for unsupported chain")
    void getAdapterOrThrowForUnsupportedChain() {
        assertThatThrownBy(() -> factory.getAdapterOrThrow(ChainType.ARBITRUM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported chain type");
    }

    @Test
    @DisplayName("isSupported should return true for registered chains")
    void isSupportedForRegisteredChain() {
        assertThat(factory.isSupported(ChainType.ETHEREUM)).isTrue();
        assertThat(factory.isSupported(ChainType.BSC)).isTrue();
    }

    @Test
    @DisplayName("isSupported should return false for unregistered chains")
    void isSupportedForUnregisteredChain() {
        assertThat(factory.isSupported(ChainType.POLYGON)).isFalse();
        assertThat(factory.isSupported(ChainType.ARBITRUM)).isFalse();
    }

    @Test
    @DisplayName("getSupportedChains should return all registered chain types")
    void getSupportedChains() {
        List<ChainType> supported = factory.getSupportedChains();

        assertThat(supported).containsExactlyInAnyOrder(ChainType.ETHEREUM, ChainType.BSC);
    }

    // Test implementation of ChainAdapter
    private static class TestChainAdapter implements ChainAdapter {
        private final ChainType chainType;

        TestChainAdapter(ChainType chainType) {
            this.chainType = chainType;
        }

        @Override
        public ChainType getChainType() {
            return chainType;
        }

        @Override
        public boolean isValidAddress(String address) {
            return true;
        }

        @Override
        public String normalizeAddress(String address) {
            return address;
        }

        @Override
        public Balance getNativeBalance(String address) {
            return null;
        }

        @Override
        public List<TokenBalance> getTokenBalances(String address, List<String> tokenAddresses) {
            return List.of();
        }

        @Override
        public BigDecimal getGasPrice() {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }
}
