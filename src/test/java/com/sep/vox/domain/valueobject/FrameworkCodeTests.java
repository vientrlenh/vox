package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class FrameworkCodeTests {

    @Test
    void should_accept_valid_framework_code() {
        var code = new FrameworkCode("CEFR_EN");
        assertThat(code.value()).isEqualTo("CEFR_EN");
    }

    @Test
    void should_accept_null_code() {
        var code = new FrameworkCode(null);
        assertThat(code.value()).isNull();
    }

    @Test
    void should_reject_lowercase_code() {
        assertThrows(IllegalArgumentException.class, () -> new FrameworkCode("cefr-en"));
    }

    @Test
    void should_reject_empty_string() {
        assertThrows(IllegalArgumentException.class, () -> new FrameworkCode(""));
    }

    @Test
    void should_reject_code_with_spaces() {
        assertThrows(IllegalArgumentException.class, () -> new FrameworkCode("CEFR EN"));
    }

    @Test
    void should_reject_code_with_invalid_characters() {
        assertThrows(IllegalArgumentException.class, () -> new FrameworkCode("CEFR@2024"));
    }

    @Test
    void should_accept_code_with_dash_and_underscore() {
        var code1 = new FrameworkCode("CEFR-2024");
        var code2 = new FrameworkCode("CEFR_2024");
        assertThat(code1.value()).isEqualTo("CEFR-2024");
        assertThat(code2.value()).isEqualTo("CEFR_2024");
    }
}
