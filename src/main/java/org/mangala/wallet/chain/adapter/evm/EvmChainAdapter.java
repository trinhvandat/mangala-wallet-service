package org.mangala.wallet.chain.adapter.evm;

import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.chain.adapter.ChainAdapter;
import org.mangala.wallet.chain.config.ChainProperties;
import org.mangala.wallet.chain.domain.Balance;
import org.mangala.wallet.chain.domain.ChainType;
import org.mangala.wallet.chain.domain.TokenBalance;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * EVM-compatible chain adapter using Web3j.
 * Supports Ethereum, BSC, Polygon, Arbitrum, and other EVM chains.
 */
@Slf4j
public class EvmChainAdapter implements ChainAdapter {

    private static final int NATIVE_TOKEN_DECIMALS = 18;
    private static final BigDecimal WEI_TO_ETH = BigDecimal.TEN.pow(NATIVE_TOKEN_DECIMALS);

    private final ChainType chainType;
    private final Web3j web3j;
    private final EvmAddressValidator addressValidator;
    private final String nativeTokenName;

    public EvmChainAdapter(ChainType chainType, ChainProperties chainProperties, EvmAddressValidator addressValidator) {
        this.chainType = chainType;
        this.addressValidator = addressValidator;
        this.nativeTokenName = getNativeTokenName(chainType);

        String rpcUrl = chainProperties.getRpcUrl(chainType);
        if (rpcUrl == null || rpcUrl.isBlank()) {
            throw new IllegalArgumentException("RPC URL not configured for chain: " + chainType);
        }
        this.web3j = Web3j.build(new HttpService(rpcUrl));
    }

    @Override
    public ChainType getChainType() {
        return chainType;
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
        try {
            BigInteger balanceWei = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                    .send()
                    .getBalance();

            BigDecimal balanceEth = new BigDecimal(balanceWei)
                    .divide(WEI_TO_ETH, 18, RoundingMode.DOWN);

            return Balance.builder()
                    .symbol(chainType.getNativeSymbol())
                    .name(nativeTokenName)
                    .decimals(NATIVE_TOKEN_DECIMALS)
                    .balanceRaw(balanceWei)
                    .balance(balanceEth)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get native balance for {} on {}: {}", address, chainType, e.getMessage());
            throw new RuntimeException("Failed to get balance: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TokenBalance> getTokenBalances(String address, List<String> tokenAddresses) {
        List<TokenBalance> balances = new ArrayList<>();

        // Always include native token balance
        try {
            Balance nativeBalance = getNativeBalance(address);
            balances.add(TokenBalance.builder()
                    .tokenAddress(TokenBalance.NATIVE_TOKEN_ADDRESS)
                    .symbol(nativeBalance.getSymbol())
                    .name(nativeBalance.getName())
                    .decimals(nativeBalance.getDecimals())
                    .balanceRaw(nativeBalance.getBalanceRaw())
                    .balance(nativeBalance.getBalance())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to get native balance for {}: {}", address, e.getMessage());
        }

        // TODO: Implement ERC-20 token balance fetching
        // This requires calling balanceOf on each token contract
        // Will be implemented in a future task with batch multicall support

        return balances;
    }

    @Override
    public BigDecimal getGasPrice() {
        try {
            BigInteger gasPriceWei = web3j.ethGasPrice().send().getGasPrice();
            // Return gas price in Gwei for readability
            return new BigDecimal(gasPriceWei).divide(BigDecimal.TEN.pow(9), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("Failed to get gas price for {}: {}", chainType, e.getMessage());
            throw new RuntimeException("Failed to get gas price: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            web3j.ethBlockNumber().send();
            return true;
        } catch (Exception e) {
            log.warn("Chain {} is not available: {}", chainType, e.getMessage());
            return false;
        }
    }

    private String getNativeTokenName(ChainType chainType) {
        return switch (chainType) {
            case ETHEREUM -> "Ethereum";
            case BSC -> "BNB";
            case POLYGON -> "Polygon";
            case ARBITRUM -> "Ethereum (Arbitrum)";
        };
    }
}
