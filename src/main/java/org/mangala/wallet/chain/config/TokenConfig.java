package org.mangala.wallet.chain.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.chain.domain.ChainType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Loads and provides token configuration from tokens.json.
 * Used for ERC-20 balance fetching to know which tokens to query.
 */
@Slf4j
@Component
public class TokenConfig {

    private Map<ChainType, ChainTokenConfig> chainTokens = new EnumMap<>(ChainType.class);

    @PostConstruct
    public void init() {
        loadTokenConfig();
    }

    private void loadTokenConfig() {
        try {
            ClassPathResource resource = new ClassPathResource("config/tokens.json");
            InputStream inputStream = resource.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            TokensFile tokensFile = mapper.readValue(inputStream, TokensFile.class);

            // Map each chain from the JSON file
            mapChain(tokensFile.ethereum, ChainType.ETHEREUM);
            mapChain(tokensFile.bsc, ChainType.BSC);
            mapChain(tokensFile.polygon, ChainType.POLYGON);
            mapChain(tokensFile.arbitrum, ChainType.ARBITRUM);

            log.info("Loaded token config for {} chains", chainTokens.size());
            chainTokens.forEach((chain, config) ->
                    log.debug("Chain {}: {} tokens configured", chain, config.getTokens().size()));

        } catch (IOException e) {
            log.error("Failed to load tokens.json: {}", e.getMessage());
            throw new RuntimeException("Failed to load token configuration", e);
        }
    }

    private void mapChain(ChainTokensJson chainJson, ChainType chainType) {
        if (chainJson == null) {
            return;
        }

        ChainTokenConfig config = new ChainTokenConfig();
        config.setChainId(chainJson.chainId);

        if (chainJson.nativeToken != null) {
            config.setNativeSymbol(chainJson.nativeToken.symbol);
            config.setNativeDecimals(chainJson.nativeToken.decimals);
            config.setNativeCoingeckoId(chainJson.nativeToken.coingeckoId);
        }

        List<TokenInfo> tokens = new ArrayList<>();
        if (chainJson.tokens != null) {
            for (TokenJson tokenJson : chainJson.tokens) {
                tokens.add(TokenInfo.builder()
                        .symbol(tokenJson.symbol)
                        .name(tokenJson.name)
                        .address(tokenJson.address)
                        .decimals(tokenJson.decimals)
                        .coingeckoId(tokenJson.coingeckoId)
                        .build());
            }
        }
        config.setTokens(tokens);

        chainTokens.put(chainType, config);
    }

    /**
     * Get token configuration for a specific chain.
     */
    public ChainTokenConfig getChainConfig(ChainType chainType) {
        return chainTokens.get(chainType);
    }

    /**
     * Get list of token addresses to query for a chain.
     */
    public List<String> getTokenAddresses(ChainType chainType) {
        ChainTokenConfig config = chainTokens.get(chainType);
        if (config == null || config.getTokens() == null) {
            return Collections.emptyList();
        }
        return config.getTokens().stream()
                .map(TokenInfo::getAddress)
                .toList();
    }

    /**
     * Get token info by contract address.
     */
    public Optional<TokenInfo> getTokenByAddress(ChainType chainType, String address) {
        ChainTokenConfig config = chainTokens.get(chainType);
        if (config == null || config.getTokens() == null) {
            return Optional.empty();
        }
        return config.getTokens().stream()
                .filter(t -> t.getAddress().equalsIgnoreCase(address))
                .findFirst();
    }

    // ===== Data classes for JSON parsing =====

    @Data
    public static class ChainTokenConfig {
        private int chainId;
        private String nativeSymbol;
        private int nativeDecimals;
        private String nativeCoingeckoId;
        private List<TokenInfo> tokens;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TokenInfo {
        private String symbol;
        private String name;
        private String address;
        private int decimals;
        private String coingeckoId;
    }

    // ===== JSON mapping classes =====

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokensFile {
        public ChainTokensJson ethereum;
        public ChainTokensJson bsc;
        public ChainTokensJson polygon;
        public ChainTokensJson arbitrum;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChainTokensJson {
        public int chainId;
        public NativeTokenJson nativeToken;
        public List<TokenJson> tokens;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NativeTokenJson {
        public String symbol;
        public int decimals;
        public String coingeckoId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenJson {
        public String symbol;
        public String name;
        public String address;
        public int decimals;
        public String coingeckoId;
    }
}
