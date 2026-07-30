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

    // Định dạng kiểu Mỹ đã bị bỏ có chủ đích: khi cả dd/MM và MM/dd cùng được nhận thì "03/04/2026"
    // và "04/13/2026" cho ra hai cách hiểu khác nhau mà không hề báo lỗi -- ngày sai âm thầm còn tệ
    // hơn một request bị từ chối.
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
     * Bảng này là lý do tồn tại của việc gọi thẳng {@code OffsetDateTime.parse} thay vì so với một
     * danh sách {@code ofPattern} viết tay. Mỗi dòng ở đây từng bị một danh sách pattern như thế từ
     * chối, nên nếu ai đó thu hẹp tập chấp nhận lại thì phải thấy test đỏ chứ không phải thấy một
     * lỗi 400 lúc người dùng bấm lưu.
     */
    @Nested
    @DisplayName("toInstant: input tự mang offset")
    class SelfDescribingInput {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            // dạng chuẩn RFC 3339
            "2026-07-28T09:30:00+07:00, 2026-07-28T02:30:00Z",
            "2026-07-28T09:30:00Z,      2026-07-28T09:30:00Z",
            "2026-07-28T09:30:00-05:00, 2026-07-28T14:30:00Z",
            // giây là tùy chọn trong ISO-8601
            "2026-07-28T09:30+07:00,    2026-07-28T02:30:00Z",
            // offset dạng ngắn
            "2026-07-28T09:30:00+07,    2026-07-28T02:30:00Z",
            // chữ 't' thường vẫn hợp lệ theo ISO-8601
            "2026-07-28t09:30:00Z,      2026-07-28T09:30:00Z",
            // khoảng trắng hai đầu phải được bỏ qua
            "'  2026-07-28T09:30:00Z  ', 2026-07-28T09:30:00Z",
        })
        void should_accept_every_iso_offset_spelling(String raw, String expected) {
            assertThat(DateMapper.toInstant(raw)).isEqualTo(Instant.parse(expected));
        }

        // Đây là output của new Date().toISOString() trong JavaScript -- payload mà web client thật
        // sự gửi. Một danh sách pattern "yyyy-MM-dd'T'HH:mm:ssXXX" sẽ từ chối chính dòng này.
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
            // Cùng một khoảnh khắc, hai cách viết. Đây là điều khiến client ở múi giờ nào cũng đúng.
            assertThat(DateMapper.toInstant("2026-07-28T10:00:00+07:00"))
                .isEqualTo(DateMapper.toInstant("2026-07-28T03:00:00.000Z"));
        }

        @Test
        void should_ignore_default_zone_when_input_carries_an_offset() {
            // +07:00 mặc định không được phép chen vào khi input đã tự nói rõ múi giờ của nó.
            assertThat(DateMapper.toInstant("2026-07-28T09:30:00-05:00"))
                .isEqualTo(Instant.parse("2026-07-28T14:30:00Z"));
        }
    }

    @Nested
    @DisplayName("toInstant: nhánh tương thích cho input không có offset")
    class ZonelessInput {

        // Web client đang gửi date-only từ <input type="date">. Nhánh này giữ cho luồng đó không
        // chết trong lúc client chưa chuyển sang ISO có offset -- xóa test này cùng lúc xóa nhánh đó.
        @Test
        void should_resolve_date_only_at_start_of_day_in_default_zone() {
            assertThat(DateMapper.toInstant("2026-07-28"))
                .isEqualTo(Instant.parse("2026-07-27T17:00:00Z")); // 00:00 giờ Việt Nam
        }

        @Test
        void should_resolve_local_date_time_in_default_zone() {
            // Trạng thái nửa vời rất dễ xảy ra: client đổi sang <input type="datetime-local"> nhưng
            // gửi thẳng e.target.value mà quên toISOString().
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
    @DisplayName("toInstant: từ chối")
    class Rejected {

        @Test
        void should_treat_blank_as_absent_rather_than_invalid() {
            assertThat(DateMapper.toInstant(null)).isNull();
            assertThat(DateMapper.toInstant("   ")).isNull();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
            "2026-07-28 09:30:00+07:00",                  // dấu cách thay vì 'T'
            "2026-07-28T09:30:00+0700",                   // offset dạng basic, ISO_OFFSET không nhận
            "2026-07-28T09:30:00+07:00[Asia/Ho_Chi_Minh]", // có zone id, dạng của Temporal/luxon
            "2026.07.28",
            "hom nay",
        })
        void should_reject_unparseable_input(String raw) {
            assertThrows(IllegalArgumentException.class, () -> DateMapper.toInstant(raw));
        }

        // Thông báo lỗi phải nêu ví dụ, vì "phải kèm múi giờ" là ràng buộc mới mà client chưa biết.
        @Test
        void should_explain_the_expected_shape_in_the_error_message() {
            assertThatThrownBy(() -> DateMapper.toInstant("hom nay"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("múi giờ")
                .hasMessageContaining("2026-07-28T09:30:00+07:00");
        }
    }

    @Nested
    @DisplayName("toImportedInstant: đường import file")
    class ImportedInput {

        private static final ZoneId HANOI = ZoneId.of("Asia/Ho_Chi_Minh");
        private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

        // Ô Excel chỉ có ngày là hợp lệ ở đây, không phải nợ kỹ thuật: người gõ tay không có múi giờ
        // nào để kèm theo.
        @Test
        void should_accept_date_only_cell() {
            assertThat(DateMapper.toImportedInstant("28/07/2026", HANOI))
                .isEqualTo(Instant.parse("2026-07-27T17:00:00Z"));
        }

        // Đây là điều mà tham số zone bắt buộc mua được: cùng một ô, hai múi giờ, hai thời điểm khác
        // nhau -- và lựa chọn đó hiện ra ở call site chứ không nằm ẩn trong bộ parse.
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
