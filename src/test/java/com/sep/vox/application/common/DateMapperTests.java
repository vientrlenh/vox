package com.sep.vox.application.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class DateMapperTests {

    @Test
    void should_parse_supported_date_formats() {
        assertThat(DateMapper.toLocalDate("2026-05-24")).isEqualTo(LocalDate.of(2026, 5, 24));
        assertThat(DateMapper.toLocalDate("2026/05/24")).isEqualTo(LocalDate.of(2026, 5, 24));
        assertThat(DateMapper.toLocalDate("24-05-2026")).isEqualTo(LocalDate.of(2026, 5, 24));
        assertThat(DateMapper.toLocalDate("24/05/2026")).isEqualTo(LocalDate.of(2026, 5, 24));
    }

    // The US format was dropped on purpose: when both dd/MM and MM/dd are accepted, "03/04/2026"
    // and "04/13/2026" produce two different readings without any error -- a silently wrong date is
    // worse than a rejected request.
    @Test
    void should_no_longer_accept_us_month_first_formats() {
        assertThrows(IllegalArgumentException.class, () -> DateMapper.toLocalDate("05-24-2026"));
        assertThrows(IllegalArgumentException.class, () -> DateMapper.toLocalDate("05/24/2026"));
    }

    @Test
    void should_format_local_date_for_output() {
        assertThat(DateMapper.localDateToString(LocalDate.of(2026, 5, 24))).isEqualTo("24-05-2026");
    }

    @Test
    void should_reject_invalid_date_format() {
        assertThrows(IllegalArgumentException.class, () -> DateMapper.toLocalDate("2026.05.24"));
    }

    @Test
    void should_treat_blank_local_date_as_absent() {
        assertThat(DateMapper.toLocalDate(null)).isNull();
        assertThat(DateMapper.toLocalDate("   ")).isNull();
    }

    /**
     * This table is the reason for calling {@code OffsetDateTime.parse} directly instead of comparing
     * against a hand-written list of {@code ofPattern}. Every row here was once rejected by such a
     * pattern list, so if someone narrows the accepted set again the test must turn red, instead of
     * users seeing a 400 error when they press save.
     */
    @Nested
    @DisplayName("toInstant: input that carries its own offset")
    class SelfDescribingInput {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            // the standard RFC 3339 form
            "2026-07-28T09:30:00+07:00, 2026-07-28T02:30:00Z",
            "2026-07-28T09:30:00Z,      2026-07-28T09:30:00Z",
            "2026-07-28T09:30:00-05:00, 2026-07-28T14:30:00Z",
            // seconds are optional in ISO-8601
            "2026-07-28T09:30+07:00,    2026-07-28T02:30:00Z",
            // short-form offset
            "2026-07-28T09:30:00+07,    2026-07-28T02:30:00Z",
            // a lowercase 't' is still valid per ISO-8601
            "2026-07-28t09:30:00Z,      2026-07-28T09:30:00Z",
            // surrounding whitespace must be ignored
            "'  2026-07-28T09:30:00Z  ', 2026-07-28T09:30:00Z",
        })
        void should_accept_every_iso_offset_spelling(String raw, String expected) {
            assertThat(DateMapper.toInstant(raw)).isEqualTo(Instant.parse(expected));
        }

        // This is the output of new Date().toISOString() in JavaScript -- the payload the real web client
        // sends. A pattern list such as "yyyy-MM-dd'T'HH:mm:ssXXX" would reject this very line.
        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
            "2026-07-28T09:30:00.000Z",
            "2026-07-28T09:30:00.123Z",
            "2026-07-28T09:30:00.123456Z",
            "2026-07-28T09:30:00.000+07:00",
        })
        void should_accept_fractional_seconds(String raw) {
            assertThat(DateMapper.toInstant(raw)).isNotNull();
        }

        @Test
        void should_map_equivalent_spellings_to_the_same_instant() {
            // The same instant written two ways. This is what makes the client correct in any time zone.
            assertThat(DateMapper.toInstant("2026-07-28T10:00:00+07:00"))
                .isEqualTo(DateMapper.toInstant("2026-07-28T03:00:00.000Z"));
        }

        @Test
        void should_ignore_default_zone_when_input_carries_an_offset() {
            // The default +07:00 must not be allowed in when the input already states its own time zone.
            assertThat(DateMapper.toInstant("2026-07-28T09:30:00-05:00"))
                .isEqualTo(Instant.parse("2026-07-28T14:30:00Z"));
        }
    }

    @Nested
    @DisplayName("toInstant: compatibility branch for input without an offset")
    class ZonelessInput {

        // The web client sends date-only values from <input type="date">. This branch keeps that flow
        // from breaking while the client has not yet moved to ISO with an offset -- delete this test together with that branch.
        @Test
        void should_resolve_date_only_at_start_of_day_in_default_zone() {
            assertThat(DateMapper.toInstant("2026-07-28"))
                .isEqualTo(Instant.parse("2026-07-27T17:00:00Z")); // 00:00 Vietnam time
        }

        @Test
        void should_resolve_local_date_time_in_default_zone() {
            // A half-migrated state is very easy to reach: the client switches to <input type="datetime-local">
            // but sends e.target.value directly and forgets toISOString().
            assertThat(DateMapper.toInstant("2026-07-28T09:30"))
                .isEqualTo(Instant.parse("2026-07-28T02:30:00Z"));
        }

        @Test
        void should_accept_non_iso_date_only_formats_too() {
            assertThat(DateMapper.toInstant("28/07/2026"))
                .isEqualTo(Instant.parse("2026-07-27T17:00:00Z"));
        }
    }

    @Nested
    @DisplayName("toInstant: rejection")
    class Rejected {

        @Test
        void should_treat_blank_as_absent_rather_than_invalid() {
            assertThat(DateMapper.toInstant(null)).isNull();
            assertThat(DateMapper.toInstant("   ")).isNull();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
            "2026-07-28 09:30:00+07:00",                  // a space instead of 'T'
            "2026-07-28T09:30:00+0700",                   // basic-format offset, which ISO_OFFSET does not accept
            "2026-07-28T09:30:00+07:00[Asia/Ho_Chi_Minh]", // has a zone id, the Temporal/luxon form
            "2026.07.28",
            "hom nay",
        })
        void should_reject_unparseable_input(String raw) {
            assertThrows(IllegalArgumentException.class, () -> DateMapper.toInstant(raw));
        }

        // The error message must include an example, because "must include a time zone" is a new constraint the client does not know yet.
        @Test
        void should_explain_the_expected_shape_in_the_error_message() {
            assertThatThrownBy(() -> DateMapper.toInstant("hom nay"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("time zone")
                .hasMessageContaining("2026-07-28T09:30:00+07:00");
        }
    }

    @Nested
    @DisplayName("toImportedInstant: file import path")
    class ImportedInput {

        private static final ZoneId HANOI = ZoneId.of("Asia/Ho_Chi_Minh");
        private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

        // A date-only Excel cell is valid here, and it is not technical debt: someone typing by hand has
        // no time zone to attach.
        @Test
        void should_accept_date_only_cell() {
            assertThat(DateMapper.toImportedInstant("28/07/2026", HANOI))
                .isEqualTo(Instant.parse("2026-07-27T17:00:00Z"));
        }

        // This is what the mandatory zone parameter buys: the same cell, two time zones, two different
        // instants -- and that choice is visible at the call site instead of hidden inside the parser.
        @Test
        void should_honour_the_zone_it_is_given() {
            assertThat(DateMapper.toImportedInstant("2026-07-28", NEW_YORK))
                .isEqualTo(Instant.parse("2026-07-28T04:00:00Z"))
                .isNotEqualTo(DateMapper.toImportedInstant("2026-07-28", HANOI));
        }

        @Test
        void should_let_an_explicit_offset_in_the_cell_win_over_the_zone() {
            assertThat(DateMapper.toImportedInstant("2026-07-28T09:30:00+07:00", NEW_YORK))
                .isEqualTo(Instant.parse("2026-07-28T02:30:00Z"));
        }

        @Test
        void should_treat_blank_cell_as_absent() {
            assertThat(DateMapper.toImportedInstant(null, HANOI)).isNull();
            assertThat(DateMapper.toImportedInstant("  ", HANOI)).isNull();
        }

        @Test
        void should_reject_unparseable_cell() {
            assertThrows(IllegalArgumentException.class,
                () -> DateMapper.toImportedInstant("2026.07.28", HANOI));
        }
    }
}
