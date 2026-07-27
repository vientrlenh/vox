package com.sep.vox.interfaces.kafka.mapper;

import java.time.OffsetDateTime;
import java.util.UUID;

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
            OffsetDateTime.parse(dto.occurredAt())
        );
    }

    private static ExamRequiredStreamType toStreamType(String raw) {
        var type = ExamRequiredStreamType.valueOf(raw.trim().toUpperCase());
        if (type == ExamRequiredStreamType.CAMERA_AND_SCREEN) {
            throw new IllegalArgumentException("Một bản ghi không thể có streamType CAMERA_AND_SCREEN: " + raw);
        }
        return type;
    }

    // vox-streaming publishes "UPLOADING" khi upload session vừa được tạo (chưa có gì để assemble).
    // Map về PROCESSING vì ExamRecordingAssemblyStatus (và CHECK constraint của bảng) không có giá
    // trị UPLOADING riêng -- PROCESSING là trạng thái "đang ghi/chưa có kết quả cuối" chung nhất.
    private static ExamRecordingAssemblyStatus toStatus(String raw) {
        if ("UPLOADING".equalsIgnoreCase(raw)) {
            return ExamRecordingAssemblyStatus.PROCESSING;
        }
        return ExamRecordingAssemblyStatus.valueOf(raw.trim().toUpperCase());
    }
}
