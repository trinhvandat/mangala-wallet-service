package org.mangala.wallet.price;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of PriceService for development and testing.
 * Uses hardcoded prices that simulate realistic market values.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "portfolio.price.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockPriceService implements PriceService {

    private static final Map<String, BigDecimal> MOCK_PRICES = new ConcurrentHashMap<>();
    private static final Map<String, BigDecimal> MOCK_24H_CHANGES = new ConcurrentHashMap<>();

    static {
        // Native tokens
        MOCK_PRICES.put("ETH", new BigDecimal("3500.00"));
        MOCK_PRICES.put("BNB", new BigDecimal("580.00"));
        MOCK_PRICES.put("MATIC", new BigDecimal("0.85"));
        MOCK_PRICES.put("SOL", new BigDecimal("145.00"));

        // Common ERC-20/BEP-20 tokens
        MOCK_PRICES.put("USDT", new BigDecimal("1.00"));
        MOCK_PRICES.put("USDC", new BigDecimal("1.00"));
        MOCK_PRICES.put("DAI", new BigDecimal("1.00"));
        MOCK_PRICES.put("WBTC", new BigDecimal("65000.00"));
        MOCK_PRICES.put("LINK", new BigDecimal("18.50"));
        MOCK_PRICES.put("UNI", new BigDecimal("12.30"));
        MOCK_PRICES.put("AAVE", new BigDecimal("285.00"));

        // 24h changes (in percentage)
        MOCK_24H_CHANGES.put("ETH", new BigDecimal("2.5"));
        MOCK_24H_CHANGES.put("BNB", new BigDecimal("-1.2"));
        MOCK_24H_CHANGES.put("MATIC", new BigDecimal("4.8"));
        MOCK_24H_CHANGES.put("SOL", new BigDecimal("6.3"));
        MOCK_24H_CHANGES.put("USDT", new BigDecimal("0.01"));
        MOCK_24H_CHANGES.put("USDC", new BigDecimal("-0.02"));
        MOCK_24H_CHANGES.put("DAI", new BigDecimal("0.00"));
        MOCK_24H_CHANGES.put("WBTC", new BigDecimal("1.8"));
        MOCK_24H_CHANGES.put("LINK", new BigDecimal("3.2"));
        MOCK_24H_CHANGES.put("UNI", new BigDecimal("-2.1"));
        MOCK_24H_CHANGES.put("AAVE", new BigDecimal("5.4"));
    }

    @Override
    public BigDecimal getPrice(String symbol, String chainType) {
        String upperSymbol = symbol.toUpperCase();
        BigDecimal price = MOCK_PRICES.getOrDefault(upperSymbol, BigDecimal.ZERO);
        log.debug("Mock price for {}/{}: ${}", symbol, chainType, price);
        return price;
    }

    @Override
    public Map<String, BigDecimal> getPrices(Map<String, String> symbols) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (Map.Entry<String, String> entry : symbols.entrySet()) {
            String symbol = entry.getKey();
            String chainType = entry.getValue();
            String key = symbol + ":" + chainType;
            result.put(key, getPrice(symbol, chainType));
        }
        return result;
    }

    @Override
    public BigDecimal get24hChangePercent(String symbol, String chainType) {
        String upperSymbol = symbol.toUpperCase();
        return MOCK_24H_CHANGES.getOrDefault(upperSymbol, BigDecimal.ZERO);
    }

    /**
     * Update mock price for testing purposes.
     */
    public void setMockPrice(String symbol, BigDecimal price) {
        MOCK_PRICES.put(symbol.toUpperCase(), price);
    }

    /**
     * Update mock 24h change for testing purposes.
     */
    public void setMock24hChange(String symbol, BigDecimal changePercent) {
        MOCK_24H_CHANGES.put(symbol.toUpperCase(), changePercent);
    }
}
