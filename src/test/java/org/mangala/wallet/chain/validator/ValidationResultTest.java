package org.mangala.wallet.chain.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationResult")
class ValidationResultTest {

    @Test
    @DisplayName("success() should return valid result with no error details")
    void successShouldReturnValidResult() {
        ValidationResult result = ValidationResult.success();

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorCode()).isNull();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("failure() should return invalid result with error details")
    void failureShouldReturnInvalidResult() {
        String errorCode = "TEST_ERROR";
        String errorMessage = "Test error message";

        ValidationResult result = ValidationResult.failure(errorCode, errorMessage);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(errorCode);
        assertThat(result.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("should have all error code constants defined")
    void shouldHaveErrorCodeConstants() {
        assertThat(ValidationResult.ERROR_NULL_OR_EMPTY).isEqualTo("ADDRESS_NULL_OR_EMPTY");
        assertThat(ValidationResult.ERROR_INVALID_FORMAT).isEqualTo("ADDRESS_INVALID_FORMAT");
        assertThat(ValidationResult.ERROR_INVALID_LENGTH).isEqualTo("ADDRESS_INVALID_LENGTH");
        assertThat(ValidationResult.ERROR_INVALID_CHECKSUM).isEqualTo("ADDRESS_INVALID_CHECKSUM");
        assertThat(ValidationResult.ERROR_UNSUPPORTED_CHAIN).isEqualTo("CHAIN_NOT_SUPPORTED");
    }

    @Test
    @DisplayName("builder should create result with all fields")
    void builderShouldCreateResultWithAllFields() {
        ValidationResult result = ValidationResult.builder()
                .valid(false)
                .errorCode("CUSTOM_ERROR")
                .errorMessage("Custom message")
                .build();

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("CUSTOM_ERROR");
        assertThat(result.getErrorMessage()).isEqualTo("Custom message");
    }
}
