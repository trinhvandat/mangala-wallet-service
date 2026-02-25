package org.mangala.wallet.chain.adapter;

import org.mangala.wallet.chain.domain.ChainType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory for resolving chain adapters by chain type.
 * Enables extensibility - new chain adapters are automatically registered.
 */
@Component
public class ChainAdapterFactory implements ChainAdapterResolver {

    private final Map<ChainType, ChainAdapter> adapters;

    public ChainAdapterFactory(List<ChainAdapter> chainAdapters) {
        this.adapters = new EnumMap<>(ChainType.class);
        for (ChainAdapter adapter : chainAdapters) {
            adapters.put(adapter.getChainType(), adapter);
        }
    }

    /**
     * Gets the adapter for a specific chain type.
     * @param chainType The chain type
     * @return Optional containing the adapter, or empty if not supported
     */
    public Optional<ChainAdapter> getAdapter(ChainType chainType) {
        return Optional.ofNullable(adapters.get(chainType));
    }

    /**
     * Gets the adapter for a specific chain type, throwing if not found.
     * @param chainType The chain type
     * @return The adapter
     * @throws IllegalArgumentException if chain type is not supported
     */
    public ChainAdapter getAdapterOrThrow(ChainType chainType) {
        return getAdapter(chainType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported chain type: " + chainType));
    }

    /**
     * Checks if a chain type is supported.
     * @param chainType The chain type
     * @return true if supported
     */
    public boolean isSupported(ChainType chainType) {
        return adapters.containsKey(chainType);
    }

    /**
     * Gets all supported chain types.
     * @return List of supported chain types
     */
    public List<ChainType> getSupportedChains() {
        return List.copyOf(adapters.keySet());
    }
}
