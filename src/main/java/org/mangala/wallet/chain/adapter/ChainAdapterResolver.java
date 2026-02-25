package org.mangala.wallet.chain.adapter;

import org.mangala.wallet.chain.domain.ChainType;

import java.util.List;
import java.util.Optional;

/**
 * Interface for resolving chain adapters by chain type.
 */
public interface ChainAdapterResolver {

    /**
     * Gets the adapter for a specific chain type.
     * @param chainType The chain type
     * @return Optional containing the adapter, or empty if not supported
     */
    Optional<ChainAdapter> getAdapter(ChainType chainType);

    /**
     * Gets the adapter for a specific chain type, throwing if not found.
     * @param chainType The chain type
     * @return The adapter
     * @throws IllegalArgumentException if chain type is not supported
     */
    ChainAdapter getAdapterOrThrow(ChainType chainType);

    /**
     * Checks if a chain type is supported.
     * @param chainType The chain type
     * @return true if supported
     */
    boolean isSupported(ChainType chainType);

    /**
     * Gets all supported chain types.
     * @return List of supported chain types
     */
    List<ChainType> getSupportedChains();
}
