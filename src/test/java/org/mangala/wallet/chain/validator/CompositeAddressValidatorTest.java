package org.mangala.wallet.chain.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mangala.wallet.chain.adapter.evm.EvmAddressValidator;
import org.mangala.wallet.chain.adapter.solana.SolanaAddressValidator;
import org.mangala.wallet.chain.domain.ChainType;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompositeAddressValidator")
class CompositeAddressValidatorTest {

    private CompositeAddressValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CompositeAddressValidator(
                new EvmAddressValidator(),
                new SolanaAddressValidator());
    }

    @Nested
    @DisplayName("validate - EVM chains")
    class ValidateEvmChains {

        @ParameterizedTest
        @EnumSource(value = ChainType.class, names = {"ETHEREUM", "BSC", "POLYGON", "ARBITRUM", "SEPOLIA"})
        @DisplayName("should return success for valid EVM address on all EVM chains")
        void shouldValidateEvmAddressOnAllEvmChains(ChainType chainType) {
            String address = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e";

            ValidationResult result = validator.validate(address, chainType);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("should return failure for invalid EVM address")
        void shouldReturnFailureForInvalidEvmAddress() {
            String invalidAddress = "invalid-address";

            ValidationResult result = validator.validate(invalidAddress, ChainType.ETHEREUM);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ValidationResult.ERROR_INVALID_FORMAT);
            assertThat(result.getErrorMessage()).contains("Invalid EVM address format");
        }

        @Test
        @DisplayName("should return failure for EVM address without 0x prefix")
        void shouldReturnFailureForMissingPrefix() {
            String address = "742d35Cc6634C0532925a3b844Bc454e4438f44e";

            ValidationResult result = validator.validate(address, ChainType.ETHEREUM);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ValidationResult.ERROR_INVALID_FORMAT);
        }

        @Test
        @DisplayName("should return failure for EVM address with wrong length")
        void shouldReturnFailureForWrongLength() {
            String address = "0x742d35Cc6634C0532925a3b844Bc454e4438f4"; // 39 chars

            ValidationResult result = validator.validate(address, ChainType.BSC);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ValidationResult.ERROR_INVALID_FORMAT);
        }
    }

    @Nested
    @DisplayName("validate - Solana chain")
    class ValidateSolanaChain {

        @Test
        @DisplayName("should return success for valid Solana address")
        void shouldValidateSolanaAddress() {
            String address = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";

            ValidationResult result = validator.validate(address, ChainType.SOLANA);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrorCode()).isNull();
        }

        @Test
        @DisplayName("should return success for 32-character Solana address")
        void shouldValidateMinLengthSolanaAddress() {
            String address = "11111111111111111111111111111111";

            ValidationResult result = validator.validate(address, ChainType.SOLANA);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should return failure for invalid Solana address")
        void shouldReturnFailureForInvalidSolanaAddress() {
            String invalidAddress = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e"; // EVM address

            ValidationResult result = validator.validate(invalidAddress, ChainType.SOLANA);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ValidationResult.ERROR_INVALID_FORMAT);
            assertThat(result.getErrorMessage()).contains("Invalid Solana address format");
        }

        @Test
        @DisplayName("should return failure for Solana address with invalid base58 characters")
        void shouldReturnFailureForInvalidBase58Chars() {
            String address = "0111111111111111111111111111111"; // contains 0, not valid base58

            ValidationResult result = validator.validate(address, ChainType.SOLANA);

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("should return failure for Solana address too short")
        void shouldReturnFailureForTooShort() {
            String address = "1111111111111111111111111111111"; // 31 chars

            ValidationResult result = validator.validate(address, ChainType.SOLANA);

            assertThat(result.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("validate - null and empty inputs")
    class ValidateNullAndEmpty {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should return failure for null or blank address")
        void shouldReturnFailureForNullOrBlankAddress(String address) {
            ValidationResult result = validator.validate(address, ChainType.ETHEREUM);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ValidationResult.ERROR_NULL_OR_EMPTY);
            assertThat(result.getErrorMessage()).contains("cannot be null or empty");
        }

        @Test
        @DisplayName("should return failure for null chain type")
        void shouldReturnFailureForNullChainType() {
            String address = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e";

            ValidationResult result = validator.validate(address, null);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ValidationResult.ERROR_UNSUPPORTED_CHAIN);
            assertThat(result.getErrorMessage()).contains("cannot be null");
        }
    }

    @Nested
    @DisplayName("supports")
    class Supports {

        @ParameterizedTest
        @EnumSource(value = ChainType.class, names = {"ETHEREUM", "BSC", "POLYGON", "ARBITRUM", "SEPOLIA", "SOLANA"})
        @DisplayName("should return true for all supported chains")
        void shouldSupportAllChains(ChainType chainType) {
            assertThat(validator.supports(chainType)).isTrue();
        }

        @Test
        @DisplayName("should return false for null chain type")
        void shouldReturnFalseForNull() {
            assertThat(validator.supports(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("cross-chain validation")
    class CrossChainValidation {

        @Test
        @DisplayName("EVM address should fail on Solana chain")
        void evmAddressShouldFailOnSolana() {
            String evmAddress = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e";

            ValidationResult result = validator.validate(evmAddress, ChainType.SOLANA);

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("Solana address should fail on EVM chains")
        void solanaAddressShouldFailOnEvm() {
            String solanaAddress = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";

            ValidationResult result = validator.validate(solanaAddress, ChainType.ETHEREUM);

            assertThat(result.isValid()).isFalse();
        }
    }
}
