package com.sep.vox.application.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.sep.vox.domain.common.ZoneConstant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class DateMapper {

    private DateMapper() {
    }

    private static final List<String> INPUT_LOCALDATE_FORMAT = List.of(
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy"
    );

    /**
     * Formatter được dựng sẵn một lần. {@link DateTimeFormatter#ofPattern} phải biên dịch lại chuỗi
     * pattern mỗi lần gọi, nên tạo nó bên trong vòng lặp parse là trả giá đó cho mọi request.
     */
    private static final List<DateTimeFormatter> INPUT_LOCALDATE_FORMATTERS =
            INPUT_LOCALDATE_FORMAT.stream().map(DateTimeFormatter::ofPattern).toList();

    private static final DateTimeFormatter OUTPUT_LOCALDATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Múi giờ nghiệp vụ của hệ thống. Dùng cho hai việc:
     *
     * <ul>
     *   <li><b>Giải nghĩa input không mang offset</b> — xem {@link #toInstant(String)} và
     *       {@link #toImportedInstant(String, ZoneId)}.
     *   <li><b>Hiển thị thời gian cho người dùng</b> (mail, CSV, chuỗi ngày). Một {@link Instant}
     *       tự nó <b>không</b> format ra được — nó không mang múi giờ nào cả, nên chỗ format bắt
     *       buộc phải cấp một zone. Đừng cấp bằng múi giờ mặc định của JVM: hiện nó đúng chỉ vì
     *       {@code Dockerfile} đặt {@code TZ=Asia/Ho_Chi_Minh} và {@code build.gradle} đặt
     *       {@code user.timezone} cho task {@code test} — đổi một trong hai chỗ đó là giờ hiển thị
     *       lệch mà không có test nào đỏ. Gắn zone tường minh thì không phụ thuộc vào chúng.
     * </ul>
     *
     * <p>Cố tình để {@code public}: mọi chỗ cần nó phải truyền tay vào
     * {@link #toImportedInstant(String, ZoneId)} hoặc {@code DateTimeFormatter.withZone(...)}, để
     * lựa chọn múi giờ hiện ra ở call site chứ không nằm ẩn trong bộ parse/format. Khi
     * {@code School} có cột múi giờ riêng, grep hằng này là ra đúng danh sách những chỗ phải đổi
     * sang múi giờ của trường.
     *
     * <p>Lưu ý khi xoá nhánh tương thích trong {@link #toInstant(String)}: hằng này <b>không</b>
     * biến mất theo, vì đường import và đường hiển thị vẫn cần nó.
     *
     * <p>Dùng {@link ZoneId} thay vì {@code ZoneOffset.ofHours(7)} vì {@code atStartOfDay} cần
     * ZoneId, và offset cứng sẽ sai ở những vùng có giờ mùa hè.
     *
     * <p>Giá trị thật nằm ở {@link ZoneConstant#BUSINESS_ZONE}: domain model cũng cần đúng múi này
     * (vd {@code SubscriptionPlan.endDateFrom}) mà domain không được phụ thuộc lên application, nên
     * nguồn sự thật phải nằm ở tầng trong. Giữ tên cũ ở đây để không phải sửa mọi call site.
     */
    public static final ZoneId DEFAULT_INPUT_ZONE = ZoneConstant.BUSINESS_ZONE;

    public static LocalDate toLocalDate(String localDateString) {
        if (localDateString == null || localDateString.isBlank()) {
            return null;
        }
        String stripped = localDateString.strip();
        for (DateTimeFormatter formatter : INPUT_LOCALDATE_FORMATTERS) {
            try {
                return LocalDate.parse(stripped, formatter);
            } catch (DateTimeParseException e) {
                // Thử pattern kế tiếp.
            }
        }
        throw new IllegalArgumentException("Định dạng ngày yêu cầu không hợp lệ");
    }

    public static String localDateToString(LocalDate date) {
        return date.format(OUTPUT_LOCALDATE_FORMATTER);
    }

    /**
     * Converts a client date-time string into an absolute instant.
     *
     * <p>The preferred input carries its own offset ({@code Z} or {@code ±HH:MM}): the server then
     * guesses nothing and a client in any time zone yields the right moment. That is why this method
     * calls {@link OffsetDateTime#parse} directly instead of matching a list of patterns:
     * {@code ISO_OFFSET_DATE_TIME} already accepts optional seconds, 0-9 fractional digits, {@code Z},
     * {@code ±HH:MM} and {@code ±HH:MM:SS}, which is wider than any hand-written {@code ofPattern}
     * list and cannot miss a shape a real client sends (for example
     * {@code 2026-07-28T09:30:00.000Z} from {@code new Date().toISOString()}).
     *
     * <p>Input without an offset is still accepted for now through the compatibility branch in
     * {@link #resolveWithoutOffsetForApi(String)}.
     *
     * @return {@code null} if the input is empty
     * @throws IllegalArgumentException if the input cannot be parsed in any form
     */
    public static Instant toInstant(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isBlank()) {
            return null;
        }
        String raw = dateTimeString.strip();
        Instant withOffset = parseWithOffset(raw);
        if (withOffset != null) {
            return withOffset;
        }
        return resolveWithoutOffsetForApi(raw);
    }

    /**
     * Converts a date/time cell read from an import file into an absolute instant.
     *
     * <p>It differs from {@link #toInstant(String)} in two ways, both on purpose:
     *
     * <ul>
     *   <li>A date-only value is <b>valid</b>, not technical debt. A user who types
     *       {@code 28/07/2026} into Excel gives no time zone to read, so there is nothing to warn
     *       about; this method therefore does not log like the API compatibility branch.
     *   <li>{@code zone} is a <b>required</b> parameter. A calendar date does not identify a moment:
     *       when "28/07 starts" depends entirely on the time zone. Passing it by hand keeps the
     *       choice visible at the call site instead of hidden in the parser, and lets a grep find
     *       every place to change when {@code School} gets its own time zone column.
     * </ul>
     *
     * <p>A cell that carries an offset ignores {@code zone}: the data is already exact.
     *
     * @return {@code null} if the cell is empty
     * @throws IllegalArgumentException if the cell cannot be parsed in any form
     */
    public static Instant toImportedInstant(String dateTimeString, ZoneId zone) {
        if (dateTimeString == null || dateTimeString.isBlank()) {
            return null;
        }
        String raw = dateTimeString.strip();
        Instant withOffset = parseWithOffset(raw);
        if (withOffset != null) {
            return withOffset;
        }
        Instant resolved = resolveWithoutOffset(raw, zone);
        if (resolved == null) {
            throw new IllegalArgumentException("Định dạng ngày yêu cầu không hợp lệ");
        }
        return resolved;
    }

    /**
     * Đổi thời điểm tuyệt đối thành chuỗi trả ra API.
     *
     * <p>Dùng ISO-8601 ở UTC (hậu tố {@code Z}) — đúng thứ {@link Instant#toString()} vẫn cho ra,
     * nên gọi hàm này không đổi dữ liệu client đang nhận. Điểm khác là <b>chỗ quyết định định
     * dạng chỉ còn một</b>: trước đây mỗi mapper tự gọi {@code .toString()}, muốn đổi cách hiển
     * thị là phải đi sửa từng nơi và rất dễ sót.
     *
     * <p>Cố ý KHÔNG đổi sang {@link #DEFAULT_INPUT_ZONE}: chuỗi trả ra API là dữ liệu máy đọc,
     * client tự đổi sang giờ địa phương của người dùng. Chỉ đường hiển thị cho người (mail, CSV)
     * mới cần gắn zone — xem javadoc của {@code DEFAULT_INPUT_ZONE}.
     *
     * @return {@code null} khi {@code instant} là null, để hàm gọi khỏi phải tự kiểm
     */
    public static String toIsoString(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static Instant parseWithOffset(String raw) {
        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (DateTimeParseException notSelfDescribing) {
            return null;
        }
    }

    // TEMPORARY COMPATIBILITY BRANCH.
    //
    // The web client still sends date-only values from <input type="date"> (for example
    // "2026-07-28") for effectiveFrom/effectiveTo. Dropping this branch now would break creating and
    // editing assessment policies on both REST and GraphQL, so it exists to let the two sides deploy
    // at different times.
    //
    // REMOVE it once clients always send an offset: input without an offset will then be rejected
    // outright, and DEFAULT_INPUT_ZONE is left for the file import path only.
    private static Instant resolveWithoutOffsetForApi(String raw) {
        Instant fallback = resolveWithoutOffset(raw, DEFAULT_INPUT_ZONE);
        if (fallback == null) {
            throw new IllegalArgumentException(
                    "Thời gian phải kèm múi giờ, ví dụ 2026-07-28T09:30:00+07:00 hoặc 2026-07-28T02:30:00Z");
        }
        return fallback;
    }

    /**
     * Giải nghĩa input không mang offset theo {@code zone}: thử dạng có giờ trước, rồi đến dạng chỉ
     * có ngày.
     *
     * @return {@code null} nếu không khớp dạng nào — để hàm gọi tự quyết định thông báo lỗi
     */
    private static Instant resolveWithoutOffset(String raw, ZoneId zone) {
        try {
            return LocalDateTime.parse(raw).atZone(zone).toInstant();
        } catch (DateTimeParseException e) {
            // Thử dạng chỉ có ngày.
        }
        try {
            LocalDate date = toLocalDate(raw);
            return date == null ? null : date.atStartOfDay(zone).toInstant();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
