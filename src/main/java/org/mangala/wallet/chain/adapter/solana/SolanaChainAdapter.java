package org.mangala.wallet.chain.adapter.solana;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.chain.adapter.ChainAdapter;
import org.mangala.wallet.chain.domain.Balance;
import org.mangala.wallet.chain.domain.ChainType;
import org.mangala.wallet.chain.domain.TokenBalance;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

/**
 * Solana blockchain adapter.
 * Currently implements address validation only.
 * Balance fetching via Solana RPC will be implemented in a future iteration.
 */
@Slf4j
@RequiredArgsConstructor
public class SolanaChainAdapter implements ChainAdapter {

    private static final int SOL_DECIMALS = 9;
    private static final String NATIVE_TOKEN_NAME = "Solana";

    private final SolanaAddressValidator addressValidator;

    @Override
    public ChainType getChainType() {
        return ChainType.SOLANA;
    }

    @Override
    public boolean isValidAddress(String address) {
        return addressValidator.isValidAddress(address);
    }

    @Override
    public String normalizeAddress(String address) {
        return addressValidator.normalizeAddress(address);
    }

    @Override
    public Balance getNativeBalance(String address) {
        // TODO: Implement Solana RPC integration
        log.warn("Solana balance fetching not yet implemented for address: {}", address);
        return Balance.builder()
                .symbol(ChainType.SOLANA.getNativeSymbol())
                .name(NATIVE_TOKEN_NAME)
                .decimals(SOL_DECIMALS)
                .balanceRaw(BigInteger.ZERO)
                .balance(BigDecimal.ZERO)
                .build();
    }

    @Override
    public List<TokenBalance> getTokenBalances(String address, List<String> tokenAddresses) {
        // TODO: Implement SPL token balance fetching
        log.warn("Solana token balance fetching not yet implemented for address: {}", address);

        // Return native balance only
        Balance nativeBalance = getNativeBalance(address);
        return Collections.singletonList(TokenBalance.builder()
                .tokenAddress(TokenBalance.NATIVE_TOKEN_ADDRESS)
                .symbol(nativeBalance.getSymbol())
                .name(nativeBalance.getName())
                .decimals(nativeBalance.getDecimals())
                .balanceRaw(nativeBalance.getBalanceRaw())
                .balance(nativeBalance.getBalance())
                .build());
    }

    @Override
    public BigDecimal getGasPrice() {
        // Solana uses a different fee model (lamports per signature)
        // Return a placeholder value
        log.warn("Solana gas price not yet implemented");
        return BigDecimal.ZERO;
    }

    @Override
    public boolean isAvailable() {
        // TODO: Implement Solana RPC health check
        // For now, return true to allow address validation
        return true;
    }
}
