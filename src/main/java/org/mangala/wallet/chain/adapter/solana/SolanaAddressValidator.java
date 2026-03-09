package org.mangala.wallet.chain.adapter.solana;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validates and normalizes Solana blockchain addresses.
 * Solana addresses are base58 encoded public keys, typically 32-44 characters.
 */
@Component
public class SolanaAddressValidator {

    // Base58 alphabet (excludes 0, O, I, l to avoid ambiguity)
    private static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final Pattern BASE58_PATTERN = Pattern.compile("^[" + BASE58_ALPHABET + "]{32,44}$");

    /**
     * Validates if the address is a valid Solana address format.
     * @param address The address to validate
     * @return true if valid format
     */
    public boolean isValidAddress(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        return BASE58_PATTERN.matcher(address).matches();
    }

    /**
     * Normalizes a Solana address.
     * Solana addresses are case-sensitive, so normalization just trims whitespace.
     * @param address The address to normalize
     * @return Normalized address
     * @throws IllegalArgumentException if address is invalid
     */
    public String normalizeAddress(String address) {
        if (!isValidAddress(address)) {
            throw new IllegalArgumentException("Invalid Solana address: " + address);
        }
        return address.trim();
    }

    /**
     * Compares two Solana addresses for equality.
     * Solana addresses are case-sensitive.
     * @param address1 First address
     * @param address2 Second address
     * @return true if addresses are equal
     */
    public boolean addressEquals(String address1, String address2) {
        if (address1 == null || address2 == null) {
            return false;
        }
        return address1.equals(address2);
    }
}
