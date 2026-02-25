package org.mangala.wallet.chain.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Represents a token balance for a specific wallet address.
 */
@Data
@Builder
public class TokenBalance {
    private final String tokenAddress;  // "native" for chain native token, or contract address
    private final String symbol;
    private final String name;
    private final Integer decimals;
    private final BigInteger balanceRaw;  // Raw balance in smallest unit (wei, satoshi, etc.)
    private final BigDecimal balance;      // Human-readable balance
    private final BigDecimal priceUsd;     // Current price in USD (nullable)
    private final BigDecimal valueUsd;     // balance * priceUsd (nullable)

    public static final String NATIVE_TOKEN_ADDRESS = "native";

    public boolean isNativeToken() {
        return NATIVE_TOKEN_ADDRESS.equals(tokenAddress);
    }
}
