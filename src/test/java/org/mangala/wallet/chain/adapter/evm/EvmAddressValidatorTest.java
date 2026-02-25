package org.mangala.wallet.chain.adapter.evm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EvmAddressValidator")
class EvmAddressValidatorTest {

    private EvmAddressValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EvmAddressValidator();
    }

    @Nested
    @DisplayName("isValidAddress")
    class IsValidAddress {

        @Test
        @DisplayName("should return true for valid lowercase address")
        void validLowercaseAddress() {
            String address = "0x742d35cc6634c0532925a3b844bc454e4438f44e";
            assertThat(validator.isValidAddress(address)).isTrue();
        }

        @Test
        @DisplayName("should return true for valid uppercase address")
        void validUppercaseAddress() {
            String address = "0x742D35CC6634C0532925A3B844BC454E4438F44E";
            assertThat(validator.isValidAddress(address)).isTrue();
        }

        @Test
        @DisplayName("should return true for valid checksummed address")
        void validChecksummedAddress() {
            String address = "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed";
            assertThat(validator.isValidAddress(address)).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should return false for null or empty address")
        void nullOrEmptyAddress(String address) {
            assertThat(validator.isValidAddress(address)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "0x742d35cc6634c0532925a3b844bc454e4438f44",   // too short
                "0x742d35cc6634c0532925a3b844bc454e4438f44ee", // too long
                "742d35cc6634c0532925a3b844bc454e4438f44e",    // missing 0x
                "0xGGGd35cc6634c0532925a3b844bc454e4438f44e",  // invalid hex chars
                "invalid-address"
        })
        @DisplayName("should return false for invalid addresses")
        void invalidAddresses(String address) {
            assertThat(validator.isValidAddress(address)).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidAddressWithChecksum")
    class IsValidAddressWithChecksum {

        @Test
        @DisplayName("should return true for valid checksummed address")
        void validChecksummedAddress() {
            String address = "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed";
            assertThat(validator.isValidAddressWithChecksum(address)).isTrue();
        }

        @Test
        @DisplayName("should return true for all lowercase address")
        void allLowercaseAddress() {
            String address = "0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed";
            assertThat(validator.isValidAddressWithChecksum(address)).isTrue();
        }

        @Test
        @DisplayName("should return true for all uppercase address")
        void allUppercaseAddress() {
            String address = "0x5AAEB6053F3E94C9B9A09F33669435E7EF1BEAED";
            assertThat(validator.isValidAddressWithChecksum(address)).isTrue();
        }

        @Test
        @DisplayName("should return false for invalid checksum")
        void invalidChecksum() {
            // Wrong case in checksum
            String address = "0x5aAeb6053F3E94C9b9A09f33669435E7EF1BEAED";
            assertThat(validator.isValidAddressWithChecksum(address)).isFalse();
        }
    }

    @Nested
    @DisplayName("normalizeAddress")
    class NormalizeAddress {

        @Test
        @DisplayName("should return checksummed address for valid input")
        void normalizeValidAddress() {
            String address = "0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed";
            String normalized = validator.normalizeAddress(address);
            assertThat(normalized).isEqualTo("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed");
        }

        @Test
        @DisplayName("should throw exception for invalid address")
        void throwForInvalidAddress() {
            assertThatThrownBy(() -> validator.normalizeAddress("invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid EVM address");
        }
    }

    @Nested
    @DisplayName("addressEquals")
    class AddressEquals {

        @Test
        @DisplayName("should return true for same address with different case")
        void sameAddressDifferentCase() {
            String address1 = "0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed";
            String address2 = "0x5AAEB6053F3E94C9B9A09F33669435E7EF1BEAED";
            assertThat(validator.addressEquals(address1, address2)).isTrue();
        }

        @Test
        @DisplayName("should return false for different addresses")
        void differentAddresses() {
            String address1 = "0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed";
            String address2 = "0x742d35cc6634c0532925a3b844bc454e4438f44e";
            assertThat(validator.addressEquals(address1, address2)).isFalse();
        }

        @Test
        @DisplayName("should return false when either address is null")
        void nullAddress() {
            String address = "0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed";
            assertThat(validator.addressEquals(null, address)).isFalse();
            assertThat(validator.addressEquals(address, null)).isFalse();
        }
    }
}
