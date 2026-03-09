package org.mangala.wallet.price;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockPriceService")
class MockPriceServiceTest {

    private MockPriceService priceService;

    @BeforeEach
    void setUp() {
        priceService = new MockPriceService();
    }

    @Test
    @DisplayName("should return known price for ETH")
    void shouldReturnKnownPriceForEth() {
        BigDecimal price = priceService.getPrice("ETH", "ETHEREUM");

        assertThat(price).isEqualByComparingTo(new BigDecimal("3500.00"));
    }

    @Test
    @DisplayName("should return known price for BNB")
    void shouldReturnKnownPriceForBnb() {
        BigDecimal price = priceService.getPrice("BNB", "BSC");

        assertThat(price).isEqualByComparingTo(new BigDecimal("580.00"));
    }

    @Test
    @DisplayName("should return known price for stablecoins")
    void shouldReturnKnownPriceForStablecoins() {
        assertThat(priceService.getPrice("USDT", "ETHEREUM"))
                .isEqualByComparingTo(BigDecimal.ONE);
        assertThat(priceService.getPrice("USDC", "ETHEREUM"))
                .isEqualByComparingTo(BigDecimal.ONE);
        assertThat(priceService.getPrice("DAI", "ETHEREUM"))
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("should return zero for unknown token")
    void shouldReturnZeroForUnknownToken() {
        BigDecimal price = priceService.getPrice("UNKNOWN_TOKEN", "ETHEREUM");

        assertThat(price).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should be case insensitive")
    void shouldBeCaseInsensitive() {
        assertThat(priceService.getPrice("eth", "ETHEREUM"))
                .isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(priceService.getPrice("Eth", "ETHEREUM"))
                .isEqualByComparingTo(new BigDecimal("3500.00"));
    }

    @Test
    @DisplayName("should return 24h change for known tokens")
    void shouldReturn24hChangeForKnownTokens() {
        BigDecimal ethChange = priceService.get24hChangePercent("ETH", "ETHEREUM");
        assertThat(ethChange).isEqualByComparingTo(new BigDecimal("2.5"));

        BigDecimal solChange = priceService.get24hChangePercent("SOL", "SOLANA");
        assertThat(solChange).isEqualByComparingTo(new BigDecimal("6.3"));
    }

    @Test
    @DisplayName("should return zero 24h change for unknown tokens")
    void shouldReturnZero24hChangeForUnknownTokens() {
        BigDecimal change = priceService.get24hChangePercent("UNKNOWN", "ETHEREUM");

        assertThat(change).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should get multiple prices at once")
    void shouldGetMultiplePricesAtOnce() {
        Map<String, String> symbols = Map.of(
                "ETH", "ETHEREUM",
                "BNB", "BSC"
        );

        Map<String, BigDecimal> prices = priceService.getPrices(symbols);

        assertThat(prices).containsKeys("ETH:ETHEREUM", "BNB:BSC");
        assertThat(prices.get("ETH:ETHEREUM")).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(prices.get("BNB:BSC")).isEqualByComparingTo(new BigDecimal("580.00"));
    }

    @Test
    @DisplayName("should allow setting mock prices")
    void shouldAllowSettingMockPrices() {
        priceService.setMockPrice("TEST", new BigDecimal("123.45"));

        BigDecimal price = priceService.getPrice("TEST", "ETHEREUM");

        assertThat(price).isEqualByComparingTo(new BigDecimal("123.45"));
    }

    @Test
    @DisplayName("should allow setting mock 24h changes")
    void shouldAllowSettingMock24hChanges() {
        priceService.setMock24hChange("TEST", new BigDecimal("-5.5"));

        BigDecimal change = priceService.get24hChangePercent("TEST", "ETHEREUM");

        assertThat(change).isEqualByComparingTo(new BigDecimal("-5.5"));
    }
}
