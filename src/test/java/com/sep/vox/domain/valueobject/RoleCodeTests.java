package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class RoleCodeTests {

    @Test
    void should_accept_valid_role_code() {
        var roleCode = new RoleCode("SCHOOL_ADMIN");

        assertThat(roleCode.value()).isEqualTo("SCHOOL_ADMIN");
    }

    @Test
    void should_accept_null_role_code() {
        var roleCode = new RoleCode(null);

        assertThat(roleCode.value()).isNull();
    }

    @Test
    void should_reject_invalid_role_code() {
        assertThrows(IllegalArgumentException.class, () -> new RoleCode("school_admin"));
    }
}
