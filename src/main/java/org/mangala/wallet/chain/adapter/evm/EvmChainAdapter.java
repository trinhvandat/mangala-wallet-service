package org.mangala.wallet.chain.adapter.evm;

import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.chain.adapter.ChainAdapter;
import org.mangala.wallet.chain.config.ChainProperties;
import org.mangala.wallet.chain.config.TokenConfig;
import org.mangala.wallet.chain.domain.Balance;
import org.mangala.wallet.chain.domain.ChainType;
import org.mangala.wallet.chain.domain.TokenBalance;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * EVM-compatible chain adapter using Web3j.
 * Supports Ethereum, BSC, Polygon, Arbitrum, and other EVM chains.
 * Implements native and ERC-20 token balance fetching.
 */
@Slf4j
public class EvmChainAdapter implements ChainAdapter {

    private static final int NATIVE_TOKEN_DECIMALS = 18;
    private static final BigDecimal WEI_TO_ETH = BigDecimal.TEN.pow(NATIVE_TOKEN_DECIMALS);

    // ERC-20 balanceOf function signature
    private static final String BALANCE_OF_FUNCTION = "balanceOf";

    private final ChainType chainType;
    private final Web3j web3j;
    private final EvmAddressValidator addressValidator;
    private final TokenConfig tokenConfig;
    private final String nativeTokenName;

    public EvmChainAdapter(ChainType chainType, ChainProperties chainProperties,
                           EvmAddressValidator addressValidator, TokenConfig tokenConfig) {
        this.chainType = chainType;
        this.addressValidator = addressValidator;
        this.tokenConfig = tokenConfig;
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

        // Determine which tokens to query
        List<String> tokensToQuery = tokenAddresses;
        if (tokensToQuery == null || tokensToQuery.isEmpty()) {
            // Use default token list from config
            tokensToQuery = tokenConfig.getTokenAddresses(chainType);
        }

        // Fetch ERC-20 token balances
        for (String tokenAddress : tokensToQuery) {
            try {
                Optional<TokenBalance> tokenBalance = getErc20Balance(address, tokenAddress);
                tokenBalance.ifPresent(balances::add);
            } catch (Exception e) {
                log.warn("Failed to get balance for token {} on {}: {}",
                        tokenAddress, chainType, e.getMessage());
                // Continue with other tokens - partial failure is acceptable
            }
        }

        return balances;
    }

    /**
     * Fetches ERC-20 token balance using eth_call to balanceOf function.
     *
     * @param walletAddress The wallet address to check balance for
     * @param tokenAddress  The ERC-20 token contract address
     * @return TokenBalance if successful, empty if balance is zero or call fails
     */
    private Optional<TokenBalance> getErc20Balance(String walletAddress, String tokenAddress) {
        try {
            // Encode balanceOf(address) function call
            Function function = new Function(
                    BALANCE_OF_FUNCTION,
                    Collections.singletonList(new Address(walletAddress)),
                    Collections.singletonList(new TypeReference<Uint256>() {})
            );
            String encodedFunction = FunctionEncoder.encode(function);

            // Make eth_call
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(walletAddress, tokenAddress, encodedFunction),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                log.debug("ERC-20 balanceOf failed for token {}: {}", tokenAddress, response.getError().getMessage());
                return Optional.empty();
            }

            // Decode the result
            List<org.web3j.abi.datatypes.Type> result = FunctionReturnDecoder.decode(
                    response.getValue(),
                    function.getOutputParameters()
            );

            if (result.isEmpty()) {
                return Optional.empty();
            }

            BigInteger balanceRaw = ((Uint256) result.get(0)).getValue();

            // Skip tokens with zero balance
            if (balanceRaw.equals(BigInteger.ZERO)) {
                return Optional.empty();
            }

            // Get token metadata from config
            Optional<TokenConfig.TokenInfo> tokenInfo = tokenConfig.getTokenByAddress(chainType, tokenAddress);

            String symbol = tokenInfo.map(TokenConfig.TokenInfo::getSymbol).orElse("UNKNOWN");
            String name = tokenInfo.map(TokenConfig.TokenInfo::getName).orElse("Unknown Token");
            int decimals = tokenInfo.map(TokenConfig.TokenInfo::getDecimals).orElse(18);

            // Convert to human-readable balance
            BigDecimal divisor = BigDecimal.TEN.pow(decimals);
            BigDecimal balance = new BigDecimal(balanceRaw).divide(divisor, decimals, RoundingMode.DOWN);

            return Optional.of(TokenBalance.builder()
                    .tokenAddress(tokenAddress)
                    .symbol(symbol)
                    .name(name)
                    .decimals(decimals)
                    .balanceRaw(balanceRaw)
                    .balance(balance)
                    .build());

        } catch (Exception e) {
            log.warn("Error fetching ERC-20 balance for token {} wallet {}: {}",
                    tokenAddress, walletAddress, e.getMessage());
            return Optional.empty();
        }
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
            case SEPOLIA -> "Sepolia ETH";
            case SOLANA -> throw new IllegalArgumentException("SOLANA is not an EVM chain");
        };
    }
}
