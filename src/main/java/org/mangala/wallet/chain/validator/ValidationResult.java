package org.mangala.wallet.chain.validator;

import lombok.Builder;
import lombok.Getter;

/**
 * Result of address validation with error details.
 */
@Getter
@Builder
public class ValidationResult {

    private final boolean valid;
    private final String errorCode;
    private final String errorMessage;

    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .build();
    }

    public static ValidationResult failure(String errorCode, String errorMessage) {
        return ValidationResult.builder()
                .valid(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    // Common error codes
    public static final String ERROR_NULL_OR_EMPTY = "ADDRESS_NULL_OR_EMPTY";
    public static final String ERROR_INVALID_FORMAT = "ADDRESS_INVALID_FORMAT";
    public static final String ERROR_INVALID_LENGTH = "ADDRESS_INVALID_LENGTH";
    public static final String ERROR_INVALID_CHECKSUM = "ADDRESS_INVALID_CHECKSUM";
    public static final String ERROR_UNSUPPORTED_CHAIN = "CHAIN_NOT_SUPPORTED";
}
