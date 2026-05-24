package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PhoneTests {

    @Test
    void should_accept_valid_vietnamese_phone_number() {
        var phone = new Phone("0987654321");

        assertThat(phone.value()).isEqualTo("0987654321");
    }

    @Test
    void should_accept_null_phone_number() {
        var phone = new Phone(null);

        assertThat(phone.value()).isNull();
    }

    @Test
    void should_reject_invalid_phone_number() {
        assertThrows(IllegalArgumentException.class, () -> new Phone("12345"));
    }
}
