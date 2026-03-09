package org.mangala.wallet.price.service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Interface for price history operations.
 * Used for 24h change calculations.
 */
public interface PriceHistoryProvider {

    /**
     * Get the 24h price change percentage for a token.
     *
     * @param symbol The token symbol
     * @param chainType The chain type
     * @return The 24h change percentage
     */
    BigDecimal get24hChangePercent(String symbol, String chainType);

    /**
     * Get the price from approximately 24 hours ago.
     *
     * @param symbol The token symbol
     * @param chainType The chain type
     * @return The price from ~24h ago, or empty if not available
     */
    Optional<BigDecimal> getPrice24hAgo(String symbol, String chainType);

    /**
     * Check if we have sufficient historical data for accurate 24h calculations.
     */
    boolean hasHistoricalData(String symbol, String chainType);

    /**
     * Record current prices for all tracked tokens.
     */
    void recordCurrentPrices();

    /**
     * Clean up old price records.
     */
    int cleanupOldPrices(int hoursToKeep);
}
