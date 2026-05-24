package com.sep.vox.application.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class StringNormalizationTests {

    @Test
    void should_trim_and_collapse_spaces() {
        assertThat(StringNormalization.trimAndCollapseSpaces("  Nguyen   Van   A  "))
            .isEqualTo("Nguyen Van A");
    }

    @Test
    void should_normalize_email_and_domain_to_lowercase() {
        assertThat(StringNormalization.normalizeEmail(" Admin@Example.COM "))
            .isEqualTo("admin@example.com");
        assertThat(StringNormalization.normalizeDomain(" SCHOOL.EDU.VN "))
            .isEqualTo("school.edu.vn");
    }

    @Test
    void should_remove_common_phone_separators() {
        assertThat(StringNormalization.normalizePhone(" 098-765.43 21 "))
            .isEqualTo("0987654321");
    }

    @Test
    void should_remove_spaces_from_identity_number() {
        assertThat(StringNormalization.normalizeIdentityNumber(" 012 345 678 "))
            .isEqualTo("012345678");
    }

    @Test
    void should_remove_accents_for_search_text() {
        assertThat(StringNormalization.normalizeSearchText("  Truong   Dai hoc  "))
            .isEqualTo("Truong Dai hoc");
    }

    @Test
    void should_return_null_when_input_is_null() {
        assertThat(StringNormalization.trimAndCollapseSpaces(null)).isNull();
        assertThat(StringNormalization.normalizeEmail(null)).isNull();
        assertThat(StringNormalization.normalizeDomain(null)).isNull();
        assertThat(StringNormalization.normalizePhone(null)).isNull();
        assertThat(StringNormalization.normalizeIdentityNumber(null)).isNull();
        assertThat(StringNormalization.normalizeSearchText(null)).isNull();
    }
}
