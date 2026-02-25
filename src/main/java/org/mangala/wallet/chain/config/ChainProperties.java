package org.mangala.wallet.chain.config;

import lombok.Data;
import org.mangala.wallet.chain.domain.ChainType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for blockchain RPC endpoints.
 */
@Data
@Component
@ConfigurationProperties(prefix = "application.wallet")
public class ChainProperties {

    private Map<String, ChainConfig> chains = new HashMap<>();

    @Data
    public static class ChainConfig {
        private String rpcUrl;
        private String name;
        private int confirmations = 12;
        private long blockTimeMs = 12000;
    }

    public String getRpcUrl(ChainType chainType) {
        ChainConfig config = getConfig(chainType);
        return config != null ? config.getRpcUrl() : null;
    }

    public ChainConfig getConfig(ChainType chainType) {
        return chains.get(chainType.name().toLowerCase());
    }

    public boolean isChainConfigured(ChainType chainType) {
        ChainConfig config = getConfig(chainType);
        return config != null && config.getRpcUrl() != null && !config.getRpcUrl().isBlank();
    }
}
