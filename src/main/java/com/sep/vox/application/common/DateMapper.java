package com.sep.vox.application.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DateMapper {

    private static final Logger log = LoggerFactory.getLogger(DateMapper.class);

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
     * Múi giờ dùng để giải nghĩa những input <b>không</b> mang offset.
     *
     * <p>Cố tình để {@code public}: mọi chỗ cần nó phải truyền tay vào
     * {@link #toImportedInstant(String, ZoneId)}, để lựa chọn múi giờ hiện ra ở call site chứ không
     * nằm ẩn trong bộ parse. Khi {@code School} có cột múi giờ riêng, grep hằng này là ra đúng danh
     * sách những chỗ phải đổi sang múi giờ của trường.
     *
     * <p>Dùng {@link ZoneId} thay vì {@code ZoneOffset.ofHours(7)} vì {@code atStartOfDay} cần
     * ZoneId, và offset cứng sẽ sai ở những vùng có giờ mùa hè.
     */
    public static final ZoneId DEFAULT_INPUT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static LocalDate toLocalDate(String localDateString) {
        if (localDateString == null || localDateString.isBlank()) {
            return null;
        }
        var stripped = localDateString.strip();
        for (var formatter : INPUT_LOCALDATE_FORMATTERS) {
            try {
                return LocalDate.parse(stripped, formatter);
            } catch (DateTimeParseException ignored) {
                // Thử pattern kế tiếp.
            }
        }
        throw new IllegalArgumentException("Định dạng ngày yêu cầu không hợp lệ");
    }

    public static String localDateToString(LocalDate date) {
        return date.format(OUTPUT_LOCALDATE_FORMATTER);
    }

    /**
     * Đổi một chuỗi thời gian của client thành thời điểm tuyệt đối.
     *
     * <p>Đường đi mong muốn là input tự mang offset ({@code Z} hoặc {@code ±HH:MM}), vì khi đó
     * server không phải đoán gì cả và client ở múi giờ nào cũng cho ra đúng khoảnh khắc. Đây là lý do
     * hàm này gọi thẳng {@link OffsetDateTime#parse} thay vì so với một danh sách pattern:
     * {@code ISO_OFFSET_DATE_TIME} đã nhận sẵn giây tùy chọn, phần thập phân 0-9 chữ số, {@code Z},
     * {@code ±HH:MM} và {@code ±HH:MM:SS} — rộng hơn bất kỳ danh sách {@code ofPattern} viết tay nào,
     * và không thể vô tình bỏ sót dạng mà client thật sự gửi (ví dụ
     * {@code 2026-07-28T09:30:00.000Z} của {@code new Date().toISOString()}).
     *
     * <p>Input không mang offset vẫn được nhận tạm qua nhánh tương thích bên dưới, kèm cảnh báo.
     *
     * @return {@code null} nếu input rỗng
     * @throws IllegalArgumentException nếu không parse được theo bất kỳ dạng nào
     */
    public static Instant toInstant(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isBlank()) {
            return null;
        }
        var raw = dateTimeString.strip();

        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (DateTimeParseException notSelfDescribing) {
            // Không mang offset -> xuống nhánh tương thích.
        }

        // ------------------------------------------------------------------
        // NHÁNH TƯƠNG THÍCH TẠM THỜI.
        //
        // Web client đang gửi date-only từ <input type="date"> (ví dụ "2026-07-28") cho
        // effectiveFrom/effectiveTo. Bỏ nhánh này ngay bây giờ sẽ làm chết luồng tạo/sửa assessment
        // policy trên cả REST và GraphQL, nên nó tồn tại để hai bên không phải deploy đúng cùng lúc.
        //
        // XÓA nhánh này khi log cảnh báo dưới đây đã sạch: lúc đó input không mang offset sẽ bị từ
        // chối thẳng, và DEFAULT_INPUT_ZONE chỉ còn được dùng ở đường import file.
        // ------------------------------------------------------------------
        var fallback = resolveWithoutOffset(raw, DEFAULT_INPUT_ZONE);
        if (fallback == null) {
            throw new IllegalArgumentException(
                    "Thời gian phải kèm múi giờ, ví dụ 2026-07-28T09:30:00+07:00 hoặc 2026-07-28T02:30:00Z");
        }
        log.warn("Nhận thời gian không kèm múi giờ: \"{}\". Tạm giải nghĩa theo {} thành {}. "
                        + "Client cần chuyển sang ISO-8601 có offset; nhánh tương thích này sẽ bị xóa.",
                raw, DEFAULT_INPUT_ZONE, fallback);
        return fallback;
    }

    /**
     * Đổi một ô ngày/giờ đọc từ file import thành thời điểm tuyệt đối.
     *
     * <p>Khác {@link #toInstant(String)} ở hai điểm, và cả hai đều có lý do:
     *
     * <ul>
     *   <li>Giá trị chỉ có ngày là <b>hợp lệ</b>, không phải nợ kỹ thuật. Người dùng gõ
     *       {@code 28/07/2026} vào Excel thì ở đó không tồn tại múi giờ nào để lấy, nên không có gì
     *       để cảnh báo — vì thế hàm này không ghi log như nhánh tương thích của API.
     *   <li>{@code zone} là tham số <b>bắt buộc</b>. Một ngày lịch tự nó không xác định được thời
     *       điểm nào cả: "ngày 28/07 bắt đầu lúc nào" phụ thuộc hoàn toàn vào múi giờ. Bắt truyền tay
     *       là cách duy nhất giữ cho lựa chọn này hiện ra ở call site thay vì nằm ẩn trong bộ parse,
     *       và là cách để khi {@code School} có cột múi giờ riêng thì grep ra được đúng chỗ cần đổi.
     * </ul>
     *
     * <p>Ô có mang offset thì {@code zone} bị bỏ qua — dữ liệu tự nó đã đủ chính xác.
     *
     * @return {@code null} nếu ô rỗng
     * @throws IllegalArgumentException nếu không parse được theo bất kỳ dạng nào
     */
    public static Instant toImportedInstant(String dateTimeString, ZoneId zone) {
        if (dateTimeString == null || dateTimeString.isBlank()) {
            return null;
        }
        var raw = dateTimeString.strip();

        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (DateTimeParseException notSelfDescribing) {
            // Ô không mang offset -> giải nghĩa theo zone được truyền vào.
        }

        var resolved = resolveWithoutOffset(raw, zone);
        if (resolved == null) {
            throw new IllegalArgumentException("Định dạng ngày yêu cầu không hợp lệ");
        }
        return resolved;
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
        } catch (DateTimeParseException notALocalDateTime) {
            // Thử dạng chỉ có ngày.
        }
        try {
            var date = toLocalDate(raw);
            return date == null ? null : date.atStartOfDay(zone).toInstant();
        } catch (IllegalArgumentException notADate) {
            return null;
        }
    }
}
