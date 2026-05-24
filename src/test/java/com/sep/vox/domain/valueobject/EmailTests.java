package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class EmailTests {

    @Test
    void should_accept_valid_email() {
        var email = new Email("admin@example.com");

        assertThat(email.value()).isEqualTo("admin@example.com");
    }

    @Test
    void should_accept_null_email() {
        var email = new Email(null);

        assertThat(email.value()).isNull();
    }

    @Test
    void should_reject_invalid_email() {
        assertThrows(IllegalArgumentException.class, () -> new Email("admin-example.com"));
    }
}
