package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class FullNameTests {
    
    @Test
    void should_accept_valid_fullName() {
        var fullName = new FullName("Nguyễn Văn A");

        assertThat(fullName.value()).isEqualTo("Nguyễn Văn A");
    }

    @Test
    void should_accept_null_fullName() {
        var nullFullName = new FullName(null);
        assertThat(nullFullName.value()).isNull();
    }

    @Test
    void should_reject_invalid_fullName() {
        assertThrows(IllegalArgumentException.class, () -> new FullName("nguyễn Văn A"));

    }
}
