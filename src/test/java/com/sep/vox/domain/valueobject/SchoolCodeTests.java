package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SchoolCodeTests {

    @Test
    void should_accept_valid_school_code() {
        var schoolCode = new SchoolCode("HCMUS_01");

        assertThat(schoolCode.value()).isEqualTo("HCMUS_01");
    }

    @Test
    void should_accept_null_school_code() {
        var schoolCode = new SchoolCode(null);

        assertThat(schoolCode.value()).isNull();
    }

    @Test
    void should_reject_invalid_school_code() {
        assertThrows(IllegalArgumentException.class, () -> new SchoolCode("hcmus_01"));
    }
}
