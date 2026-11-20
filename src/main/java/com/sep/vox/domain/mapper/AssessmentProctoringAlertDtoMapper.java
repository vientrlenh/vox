package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.AssessmentProctoringAlertDto;
import com.sep.vox.domain.model.assessment.AssessmentProctoringAlert;

public final class AssessmentProctoringAlertDtoMapper {

    private AssessmentProctoringAlertDtoMapper() {
    }

    public static AssessmentProctoringAlertDto toDto(AssessmentProctoringAlert alert) {
        return new AssessmentProctoringAlertDto(
            alert.getId(),
            alert.getEventId(),
            alert.getAssessmentSessionId(),
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
