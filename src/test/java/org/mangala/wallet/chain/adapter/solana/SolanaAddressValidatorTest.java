package org.mangala.wallet.chain.adapter.solana;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SolanaAddressValidator")
class SolanaAddressValidatorTest {

    private SolanaAddressValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SolanaAddressValidator();
    }

    @Nested
    @DisplayName("isValidAddress")
    class IsValidAddress {

        @Test
        @DisplayName("should return true for valid Solana addresses")
        void shouldReturnTrueForValidAddresses() {
            // Real Solana addresses
            assertThat(validator.isValidAddress("11111111111111111111111111111111")).isTrue();
            assertThat(validator.isValidAddress("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")).isTrue();
            assertThat(validator.isValidAddress("So11111111111111111111111111111111111111112")).isTrue();
            assertThat(validator.isValidAddress("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v")).isTrue();
            // 44 character address
            assertThat(validator.isValidAddress("7EYnhQoR9YM3N7UoaKRoA44Uy8JeaZV3qyouov87awMs")).isTrue();
        }

        @Test
        @DisplayName("should return true for 32-44 character base58 addresses")
        void shouldReturnTrueForValidLengthAddresses() {
            // 32 characters (minimum)
            assertThat(validator.isValidAddress("11111111111111111111111111111111")).isTrue();
            // 44 characters (maximum)
            assertThat(validator.isValidAddress("7EYnhQoR9YM3N7UoaKRoA44Uy8JeaZV3qyouov87awMs")).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should return false for null or empty addresses")
        void shouldReturnFalseForNullOrEmpty(String address) {
            assertThat(validator.isValidAddress(address)).isFalse();
        }

        @Test
        @DisplayName("should return false for addresses with invalid characters")
        void shouldReturnFalseForInvalidCharacters() {
            // Contains 0 (zero) - not in base58
            assertThat(validator.isValidAddress("0111111111111111111111111111111")).isFalse();
            // Contains O (capital o) - not in base58
            assertThat(validator.isValidAddress("O111111111111111111111111111111")).isFalse();
            // Contains I (capital i) - not in base58
            assertThat(validator.isValidAddress("I111111111111111111111111111111")).isFalse();
            // Contains l (lowercase L) - not in base58
            assertThat(validator.isValidAddress("l111111111111111111111111111111")).isFalse();
            // Contains special characters
            assertThat(validator.isValidAddress("1111111111111111111111111111111!")).isFalse();
            assertThat(validator.isValidAddress("1111111111111111111111111111111+")).isFalse();
        }

        @Test
        @DisplayName("should return false for addresses too short")
        void shouldReturnFalseForTooShort() {
            // 31 characters (too short)
            assertThat(validator.isValidAddress("1111111111111111111111111111111")).isFalse();
        }

        @Test
        @DisplayName("should return false for addresses too long")
        void shouldReturnFalseForTooLong() {
            // 45 characters (too long)
            assertThat(validator.isValidAddress("111111111111111111111111111111111111111111111")).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "0x742d35Cc6634C0532925a3b844Bc9e7595f2bD70", // Ethereum address
                "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh", // Bitcoin address
                "   "  // Whitespace only
        })
        @DisplayName("should return false for non-Solana addresses")
        void shouldReturnFalseForNonSolanaAddresses(String address) {
            assertThat(validator.isValidAddress(address)).isFalse();
        }
    }

    @Nested
    @DisplayName("normalizeAddress")
    class NormalizeAddress {

        @Test
        @DisplayName("should return trimmed address for valid input")
        void shouldReturnTrimmedAddress() {
            String address = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
            assertThat(validator.normalizeAddress(address)).isEqualTo(address);
        }

        @Test
        @DisplayName("should throw exception for invalid address")
        void shouldThrowForInvalidAddress() {
            assertThatThrownBy(() -> validator.normalizeAddress("invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Solana address");
        }

        @Test
        @DisplayName("should throw exception for null address")
        void shouldThrowForNullAddress() {
            assertThatThrownBy(() -> validator.normalizeAddress(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("addressEquals")
    class AddressEquals {

        @Test
        @DisplayName("should return true for identical addresses")
        void shouldReturnTrueForIdenticalAddresses() {
            String address = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
            assertThat(validator.addressEquals(address, address)).isTrue();
        }

        @Test
        @DisplayName("should return false for different addresses")
        void shouldReturnFalseForDifferentAddresses() {
            String address1 = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
            String address2 = "So11111111111111111111111111111111111111112";
            assertThat(validator.addressEquals(address1, address2)).isFalse();
        }

        @Test
        @DisplayName("should return false for case-different addresses (Solana is case-sensitive)")
        void shouldReturnFalseForCaseDifferentAddresses() {
            String address1 = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
            String address2 = "tokenkegqfezYINWajbnbgkpfxcwubvf9ss623vq5da";
            assertThat(validator.addressEquals(address1, address2)).isFalse();
        }

        @Test
        @DisplayName("should return false when either address is null")
        void shouldReturnFalseWhenNull() {
            String address = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
            assertThat(validator.addressEquals(null, address)).isFalse();
            assertThat(validator.addressEquals(address, null)).isFalse();
            assertThat(validator.addressEquals(null, null)).isFalse();
        }
    }
}
