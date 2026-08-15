package com.sep.vox.interfaces.kafka.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.RecordProctoringAlertCommand;
import com.sep.vox.interfaces.kafka.dto.AlertRaisedEventDto;

public final class ProctoringAlertCommandMapper {

    /** Khớp cột {@code detail varchar(1024)}. Cắt bớt còn hơn để cả cảnh báo rơi vào DLT. */
    private static final int MAX_DETAIL_LENGTH = 1024;

    private ProctoringAlertCommandMapper() {
    }

    public static RecordProctoringAlertCommand toCommand(AlertRaisedEventDto dto) {
        var capturedAt = DateMapper.toInstant(dto.capturedAt());
        var raisedAt = DateMapper.toInstant(dto.raisedAt());

        return new RecordProctoringAlertCommand(
            requireEventId(dto.eventId()),
            UUID.fromString(requireText(dto.sessionId(), "sessionId")),
            toOptionalUuid(dto.participantId()),
            trimToNull(dto.streamId()),
            upperOrNull(dto.streamType()),
            requireText(dto.alertType(), "alertType").trim(),
            upperOrNull(dto.level()),
            upperOrNull(dto.source()),
            truncate(trimToNull(dto.detail())),
            toConfidence(dto.confidence()),
            dto.sequenceNo(),
            capturedAt == null ? Instant.now() : capturedAt,
            raisedAt == null ? Instant.now() : raisedAt
        );
    }

    /**
     * eventId là khoá chống ghi trùng, nên thiếu nó thì không có gì chống trùng được.
     *
     * <p>Không tự sinh thay: một id mới mỗi lần gửi lại sẽ biến mọi lần Kafka redeliver thành một
     * dòng mới, và sổ đếm số lần vi phạm sẽ phồng lên theo sự cố hạ tầng chứ không theo hành vi của
     * thí sinh. Thà từ chối message còn hơn ghi vào một con số không ai tin được.
     */
    private static String requireEventId(String raw) {
        return requireText(raw, "eventId").trim();
    }

    private static String requireText(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Cảnh báo giám sát thiếu trường bắt buộc: " + field);
        }
        return raw;
    }

    /**
     * candidateId, nếu nguồn phát biết. Rỗng hoặc không phải UUID thì trả null.
     *
     * <p>Trường này từng bị phía AI gán bằng chính id phiên thi, nên trên topic vẫn còn dữ liệu cũ
     * như vậy. Ở đây không cố sửa: một giá trị sai vẫn là UUID hợp lệ nên không phân biệt được từ
     * đây, và việc tra bù đã được làm đúng chỗ hơn -- ngay tại vox-streaming, nơi duy nhất biết ánh
     * xạ phiên thi -> thí sinh.
     */
    private static UUID toOptionalUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException notAUuid) {
            return null;
        }
    }

    /**
     * Độ tin cậy về thang [0, 1] với 4 chữ số thập phân, khớp {@code numeric(5,4)}.
     *
     * <p>Ngoài khoảng thì trả null chứ không kẹp về biên: một giá trị vô lý nghĩa là ta không biết độ
     * tin cậy, và "không biết" phải trông khác với "chắc chắn 100%".
     */
    private static BigDecimal toConfidence(Double raw) {
        if (raw == null || raw.isNaN() || raw.isInfinite()) {
            return null;
        }
        var value = BigDecimal.valueOf(raw).setScale(4, RoundingMode.HALF_UP);
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            return null;
        }
        return value;
    }

    private static String truncate(String raw) {
        if (raw == null || raw.length() <= MAX_DETAIL_LENGTH) {
            return raw;
        }
        return raw.substring(0, MAX_DETAIL_LENGTH);
    }

    private static String upperOrNull(String raw) {
        var trimmed = trimToNull(raw);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        var trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
