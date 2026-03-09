package org.mangala.wallet.price;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service for fetching token prices.
 * Implementations may use external APIs (CoinGecko, CoinMarketCap) or mock data.
 */
public interface PriceService {

    /**
     * Get the current USD price for a token.
     *
     * @param symbol The token symbol (e.g., "ETH", "BTC")
     * @param chainType The chain type (e.g., "ETHEREUM", "BSC")
     * @return The current price in USD, or BigDecimal.ZERO if not found
     */
    BigDecimal getPrice(String symbol, String chainType);

    /**
     * Get prices for multiple tokens at once.
     *
     * @param symbols Map of symbol to chainType
     * @return Map of "symbol:chainType" to price in USD
     */
    Map<String, BigDecimal> getPrices(Map<String, String> symbols);

    /**
     * Get the 24-hour price change percentage for a token.
     *
     * @param symbol The token symbol
     * @param chainType The chain type
     * @return The 24h change percentage (e.g., 5.2 for +5.2%)
     */
    BigDecimal get24hChangePercent(String symbol, String chainType);
}
