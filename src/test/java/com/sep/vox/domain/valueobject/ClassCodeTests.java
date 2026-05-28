package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ClassCodeTests {

    @Test
    void should_accept_valid_class_code() {
        var classCode = new ClassCode("ENG_10-A");

        assertThat(classCode.value()).isEqualTo("ENG_10-A");
    }

    @Test
    void should_accept_null_class_code() {
        var classCode = new ClassCode(null);

        assertThat(classCode.value()).isNull();
    }

    @Test
    void should_reject_lowercase_class_code() {
        assertThrows(IllegalArgumentException.class, () -> new ClassCode("eng_10"));
    }

    @Test
    void should_reject_class_code_with_spaces() {
        assertThrows(IllegalArgumentException.class, () -> new ClassCode("ENG 10"));
    }
}
