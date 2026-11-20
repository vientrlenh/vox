package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PostalCodeTests {

    @Test
    void should_accept_valid_postal_code() {
        var postalCode = new PostalCode("700000");

        assertThat(postalCode.value()).isEqualTo("700000");
    }

    @Test
    void should_accept_null_postal_code() {
        var postalCode = new PostalCode(null);

        assertThat(postalCode.value()).isNull();
    }

    @Test
    void should_reject_invalid_postal_code() {
        assertThrows(IllegalArgumentException.class, () -> new PostalCode("-700000"));
    }
}
