package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class IdentityNumberTests {

    @Test
    void should_accept_valid_nine_digit_identity_number() {
        var identityNumber = new IdentityNumber("123456789");

        assertThat(identityNumber.value()).isEqualTo("123456789");
    }

    @Test
    void should_accept_valid_twelve_digit_identity_number() {
        var identityNumber = new IdentityNumber("123456789012");

        assertThat(identityNumber.value()).isEqualTo("123456789012");
    }

    @Test
    void should_accept_null_identity_number() {
        var identityNumber = new IdentityNumber(null);

        assertThat(identityNumber.value()).isNull();
    }

    @Test
    void should_reject_invalid_identity_number() {
        assertThrows(IllegalArgumentException.class, () -> new IdentityNumber("1234567890"));
    }
}
