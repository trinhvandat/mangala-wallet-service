package org.mangala.wallet.chain.validator;

import org.mangala.wallet.chain.domain.ChainType;

/**
 * Interface for validating blockchain addresses.
 * Implementations should handle specific chain types.
 */
public interface AddressValidator {

    /**
     * Validates an address for a specific chain type.
     *
     * @param address   The address to validate
     * @param chainType The chain type to validate against
     * @return ValidationResult with success or failure details
     */
    ValidationResult validate(String address, ChainType chainType);

    /**
     * Checks if this validator supports the given chain type.
     *
     * @param chainType The chain type to check
     * @return true if this validator can validate addresses for the chain
     */
    boolean supports(ChainType chainType);
}
