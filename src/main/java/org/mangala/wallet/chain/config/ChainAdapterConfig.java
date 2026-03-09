package org.mangala.wallet.chain.config;

import org.mangala.wallet.chain.adapter.evm.EvmAddressValidator;
import org.mangala.wallet.chain.adapter.evm.EvmChainAdapter;
import org.mangala.wallet.chain.adapter.solana.SolanaAddressValidator;
import org.mangala.wallet.chain.adapter.solana.SolanaChainAdapter;
import org.mangala.wallet.chain.domain.ChainType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for chain adapters.
 * Registers adapters based on configured RPC URLs.
 */
@Configuration
public class ChainAdapterConfig {

    @Bean
    @ConditionalOnProperty(prefix = "application.wallet.chains.ethereum", name = "rpc-url")
    public EvmChainAdapter ethereumChainAdapter(ChainProperties properties, EvmAddressValidator validator, TokenConfig tokenConfig) {
        return new EvmChainAdapter(ChainType.ETHEREUM, properties, validator, tokenConfig);
    }

    @Bean
    @ConditionalOnProperty(prefix = "application.wallet.chains.bsc", name = "rpc-url")
    public EvmChainAdapter bscChainAdapter(ChainProperties properties, EvmAddressValidator validator, TokenConfig tokenConfig) {
        return new EvmChainAdapter(ChainType.BSC, properties, validator, tokenConfig);
    }

    @Bean
    @ConditionalOnProperty(prefix = "application.wallet.chains.polygon", name = "rpc-url")
    public EvmChainAdapter polygonChainAdapter(ChainProperties properties, EvmAddressValidator validator, TokenConfig tokenConfig) {
        return new EvmChainAdapter(ChainType.POLYGON, properties, validator, tokenConfig);
    }

    @Bean
    @ConditionalOnProperty(prefix = "application.wallet.chains.arbitrum", name = "rpc-url")
    public EvmChainAdapter arbitrumChainAdapter(ChainProperties properties, EvmAddressValidator validator, TokenConfig tokenConfig) {
        return new EvmChainAdapter(ChainType.ARBITRUM, properties, validator, tokenConfig);
    }

    @Bean
    @ConditionalOnProperty(prefix = "application.wallet.chains.sepolia", name = "rpc-url")
    public EvmChainAdapter sepoliaChainAdapter(ChainProperties properties, EvmAddressValidator validator, TokenConfig tokenConfig) {
        return new EvmChainAdapter(ChainType.SEPOLIA, properties, validator, tokenConfig);
    }

    @Bean
    @ConditionalOnProperty(prefix = "application.wallet.chains.solana", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SolanaChainAdapter solanaChainAdapter(SolanaAddressValidator validator) {
        return new SolanaChainAdapter(validator);
    }
}
