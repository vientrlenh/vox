package com.sep.vox.interfaces.kafka.mapper;

import java.util.Locale;
import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.RecordRecordingPartChangedCommand;
import com.sep.vox.domain.model.exam.ExamRecordingAssemblyStatus;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.interfaces.kafka.dto.RecordingPartChangedEventDto;

public final class RecordingPartChangedCommandMapper {

    private RecordingPartChangedCommandMapper() {
    }

    public static RecordRecordingPartChangedCommand toCommand(RecordingPartChangedEventDto dto) {
        return new RecordRecordingPartChangedCommand(
            UUID.fromString(dto.sessionId()),
            UUID.fromString(dto.participantId()),
            toStreamType(dto.streamType()),
            toStatus(dto.status()),
            dto.objectKey(),
            dto.durationSecs(),
            DateMapper.toInstant(dto.occurredAt()),
            toSource(dto.source())
        );
    }

    /**
     * source là một phần khoá tra cứu của hàng bản ghi, nên nó không được phép rỗng.
     *
     * <p>Một khoá chứa null không khớp gì trong SQL: mỗi event thiếu source sẽ không tìm thấy
     * hàng của chính nó và đẻ thêm một hàng mới, lần sau lại tiếp -- hàng nhân bản âm thầm thay
     * vì được cập nhật. UNKNOWN giữ cho khoá toàn phần, và vì nó xếp dưới mọi nguồn đã biết
     * (xem RecordingPrecedence.rankOf) nên nó không bao giờ được chọn làm bản chuẩn nếu có một
     * bản biết rõ nguồn của mình.
     */
    private static String toSource(String raw) {
        return (raw == null || raw.isBlank()) ? "UNKNOWN" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private static ExamRequiredStreamType toStreamType(String raw) {
        var type = ExamRequiredStreamType.valueOf(raw.trim().toUpperCase());
        if (type == ExamRequiredStreamType.CAMERA_AND_SCREEN) {
            throw new IllegalArgumentException("Một bản ghi không thể có streamType CAMERA_AND_SCREEN: " + raw);
        }
        return type;
    }


    private static ExamRecordingAssemblyStatus toStatus(String raw) {
        if ("UPLOADING".equalsIgnoreCase(raw)) {
            return ExamRecordingAssemblyStatus.PROCESSING;
        }
        return ExamRecordingAssemblyStatus.valueOf(raw.trim().toUpperCase());
    }
}
