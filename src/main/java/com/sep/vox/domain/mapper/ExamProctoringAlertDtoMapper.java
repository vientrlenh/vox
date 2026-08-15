package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.ExamProctoringAlertDto;
import com.sep.vox.domain.model.exam.ExamProctoringAlert;

public final class ExamProctoringAlertDtoMapper {

    private ExamProctoringAlertDtoMapper() {
    }

    public static ExamProctoringAlertDto toDto(ExamProctoringAlert alert) {
        return new ExamProctoringAlertDto(
            alert.getId(),
            alert.getEventId(),
            alert.getExamSessionId(),
            alert.getCandidateId(),
            alert.getStreamId(),
            alert.getStreamType(),
            alert.getAlertType(),
            alert.getLevel(),
            alert.getSource(),
            alert.getDetail(),
            alert.getConfidence(),
            alert.getCapturedAt() == null ? null : alert.getCapturedAt().toString(),
            alert.getRaisedAt() == null ? null : alert.getRaisedAt().toString()
        );
    }
}
