package org.mangala.wallet.chain.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@Builder
public class Balance {
    private final String symbol;
    private final String name;
    private final Integer decimals;
    private final BigInteger balanceRaw;
    private final BigDecimal balance;
}
