package com.sep.vox.interfaces.kafka.mapper;

import java.util.Locale;
import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.recording.RecordRecordingPartChangedCommand;
import com.sep.vox.domain.model.assessment.AssessmentRecordingAssemblyStatus;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
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
     * source is part of the lookup key of the recording row, so it must not be empty.
     *
     * <p>A key containing null matches nothing in SQL: every event missing source would not find
     * its own row and would spawn a new one, and again next time -- silently duplicated rows instead
     * of an update. UNKNOWN keeps the key complete, and because it ranks below every known source
     * (see RecordingPrecedence.rankOf) it is never chosen as the canonical copy if there is
     * one that knows its own source.
     */
    private static String toSource(String raw) {
        return (raw == null || raw.isBlank()) ? "UNKNOWN" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private static AssessmentRequiredStreamType toStreamType(String raw) {
        AssessmentRequiredStreamType type = AssessmentRequiredStreamType.valueOf(raw.trim().toUpperCase());
        if (type == AssessmentRequiredStreamType.CAMERA_AND_SCREEN) {
            throw new IllegalArgumentException("A recording cannot have streamType CAMERA_AND_SCREEN: " + raw);
        }
        return type;
    }


    private static AssessmentRecordingAssemblyStatus toStatus(String raw) {
        if ("UPLOADING".equalsIgnoreCase(raw)) {
            return AssessmentRecordingAssemblyStatus.PROCESSING;
        }
        return AssessmentRecordingAssemblyStatus.valueOf(raw.trim().toUpperCase());
    }
}
