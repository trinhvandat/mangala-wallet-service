package org.mangala.wallet.chain.adapter.evm;

import org.springframework.stereotype.Component;
import org.web3j.crypto.Keys;

import java.util.regex.Pattern;

/**
 * Validates and normalizes EVM-compatible blockchain addresses.
 * Supports checksum validation per EIP-55.
 */
@Component
public class EvmAddressValidator {

    private static final Pattern HEX_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    /**
     * Validates if the address is a valid EVM address format.
     * @param address The address to validate
     * @return true if valid format (with or without checksum)
     */
    public boolean isValidAddress(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        return HEX_ADDRESS_PATTERN.matcher(address).matches();
    }

    /**
     * Validates address format and checksum (EIP-55).
     * @param address The address to validate
     * @return true if valid format and valid checksum (or all lowercase/uppercase)
     */
    public boolean isValidAddressWithChecksum(String address) {
        if (!isValidAddress(address)) {
            return false;
        }
        // All lowercase or all uppercase is valid (no checksum applied)
        String hexPart = address.substring(2);
        if (hexPart.equals(hexPart.toLowerCase()) || hexPart.equals(hexPart.toUpperCase())) {
            return true;
        }
        // Mixed case requires valid checksum
        try {
            String checksummed = Keys.toChecksumAddress(address);
            return address.equals(checksummed);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Normalizes address to checksum format (EIP-55).
     * @param address The address to normalize
     * @return Checksummed address
     * @throws IllegalArgumentException if address is invalid
     */
    public String normalizeAddress(String address) {
        if (!isValidAddress(address)) {
            throw new IllegalArgumentException("Invalid EVM address: " + address);
        }
        return Keys.toChecksumAddress(address);
    }

    /**
     * Compares two addresses for equality, ignoring case.
     * @param address1 First address
     * @param address2 Second address
     * @return true if addresses are equal
     */
    public boolean addressEquals(String address1, String address2) {
        if (address1 == null || address2 == null) {
            return false;
        }
        return address1.equalsIgnoreCase(address2);
    }
}
