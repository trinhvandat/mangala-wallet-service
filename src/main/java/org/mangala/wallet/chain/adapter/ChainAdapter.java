package org.mangala.wallet.chain.adapter;

import org.mangala.wallet.chain.domain.Balance;
import org.mangala.wallet.chain.domain.ChainType;
import org.mangala.wallet.chain.domain.TokenBalance;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface for blockchain adapters. Each supported chain must implement this interface.
 * This enables extensibility - new chains can be added by implementing this interface.
 */
public interface ChainAdapter {

    /**
     * Returns the chain type this adapter handles.
     */
    ChainType getChainType();

    /**
     * Validates if the given address is valid for this chain.
     * @param address The address to validate
     * @return true if valid, false otherwise
     */
    boolean isValidAddress(String address);

    /**
     * Normalizes an address to a standard format (e.g., checksum for EVM).
     * @param address The address to normalize
     * @return Normalized address
     */
    String normalizeAddress(String address);

    /**
     * Gets the native token balance for an address.
     * @param address The wallet address
     * @return Balance of native token (ETH, BNB, MATIC, etc.)
     */
    Balance getNativeBalance(String address);

    /**
     * Gets token balances for an address.
     * @param address The wallet address
     * @param tokenAddresses List of token contract addresses to check (empty = check common tokens)
     * @return List of token balances
     */
    List<TokenBalance> getTokenBalances(String address, List<String> tokenAddresses);

    /**
     * Gets all balances (native + tokens) for an address.
     * @param address The wallet address
     * @return List of all balances including native token
     */
    default List<TokenBalance> getAllBalances(String address) {
        return getTokenBalances(address, List.of());
    }

    /**
     * Gets the current gas price for the chain.
     * @return Gas price in native token units
     */
    BigDecimal getGasPrice();

    /**
     * Checks if the chain RPC is available.
     * @return true if RPC is responsive
     */
    boolean isAvailable();
}
