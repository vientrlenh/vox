package com.sep.vox.application.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class DateMapperTests {

    @Test
    void should_parse_supported_date_formats() {
        assertThat(DateMapper.toLocalDate("2026-05-24")).isEqualTo(LocalDate.of(2026, 5, 24));
        assertThat(DateMapper.toLocalDate("24/05/2026")).isEqualTo(LocalDate.of(2026, 5, 24));
        assertThat(DateMapper.toLocalDate("05-24-2026")).isEqualTo(LocalDate.of(2026, 5, 24));
    }

    @Test
    void should_format_local_date_for_output() {
        assertThat(DateMapper.localDateToString(LocalDate.of(2026, 5, 24))).isEqualTo("24-05-2026");
    }

    @Test
    void should_reject_invalid_date_format() {
        assertThrows(IllegalArgumentException.class, () -> DateMapper.toLocalDate("2026.05.24"));
    }
}
