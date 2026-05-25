package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SchoolDomainTests {

    @Test
    void should_accept_valid_school_domain() {
        var schoolDomain = new SchoolDomain("student.hcmus.edu.vn");

        assertThat(schoolDomain.value()).isEqualTo("student.hcmus.edu.vn");
    }

    @Test
    void should_accept_null_school_domain() {
        var schoolDomain = new SchoolDomain(null);

        assertThat(schoolDomain.value()).isNull();
    }

    @Test
    void should_reject_invalid_school_domain() {
        assertThrows(IllegalArgumentException.class, () -> new SchoolDomain("student.hcmus.vn"));
    }
}
