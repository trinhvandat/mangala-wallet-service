package org.mangala.wallet.chain.validator;

import lombok.extern.slf4j.Slf4j;
import org.mangala.wallet.chain.adapter.evm.EvmAddressValidator;
import org.mangala.wallet.chain.adapter.solana.SolanaAddressValidator;
import org.mangala.wallet.chain.domain.ChainType;
import org.springframework.stereotype.Service;

/**
 * Composite address validator that routes validation to chain-specific validators.
 */
@Slf4j
@Service
public class CompositeAddressValidator implements AddressValidator {

    private final EvmAddressValidator evmAddressValidator;
    private final SolanaAddressValidator solanaAddressValidator;

    public CompositeAddressValidator(
            EvmAddressValidator evmAddressValidator,
            SolanaAddressValidator solanaAddressValidator) {
        this.evmAddressValidator = evmAddressValidator;
        this.solanaAddressValidator = solanaAddressValidator;
    }

    @Override
    public ValidationResult validate(String address, ChainType chainType) {
        log.debug("Validating address for chain: {}", chainType);

        if (address == null || address.isBlank()) {
            return ValidationResult.failure(
                    ValidationResult.ERROR_NULL_OR_EMPTY,
                    "Address cannot be null or empty");
        }

        if (chainType == null) {
            return ValidationResult.failure(
                    ValidationResult.ERROR_UNSUPPORTED_CHAIN,
                    "Chain type cannot be null");
        }

        if (chainType == ChainType.SOLANA) {
            return validateSolanaAddress(address);
        } else if (chainType.isEvm()) {
            return validateEvmAddress(address);
        }

        return ValidationResult.failure(
                ValidationResult.ERROR_UNSUPPORTED_CHAIN,
                "Unsupported chain type: " + chainType);
    }

    @Override
    public boolean supports(ChainType chainType) {
        if (chainType == null) {
            return false;
        }
        return chainType.isEvm() || chainType == ChainType.SOLANA;
    }

    private ValidationResult validateEvmAddress(String address) {
        if (!evmAddressValidator.isValidAddress(address)) {
            return ValidationResult.failure(
                    ValidationResult.ERROR_INVALID_FORMAT,
                    "Invalid EVM address format. Must be 0x followed by 40 hexadecimal characters");
        }
        return ValidationResult.success();
    }

    private ValidationResult validateSolanaAddress(String address) {
        if (!solanaAddressValidator.isValidAddress(address)) {
            return ValidationResult.failure(
                    ValidationResult.ERROR_INVALID_FORMAT,
                    "Invalid Solana address format. Must be 32-44 base58 characters");
        }
        return ValidationResult.success();
    }
}
