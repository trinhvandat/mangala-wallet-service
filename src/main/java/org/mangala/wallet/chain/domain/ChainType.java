package org.mangala.wallet.chain.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChainType {
    ETHEREUM(1, "ETH"),
    BSC(56, "BNB"),
    POLYGON(137, "MATIC"),
    ARBITRUM(42161, "ETH");

    private final int chainId;
    private final String nativeSymbol;
}
