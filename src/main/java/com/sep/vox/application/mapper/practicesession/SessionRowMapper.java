package com.sep.vox.application.mapper.practicesession;

import com.sep.vox.application.query.dto.SessionRowInfo;
import com.sep.vox.domain.dto.personalization.PracticeSessionDto;

public final class SessionRowMapper {

    private SessionRowMapper() {
    }

    public static PracticeSessionDto toDto(SessionRowInfo row) {
        if (row == null) {
            return null;
        }
        return new PracticeSessionDto(
            row.getId(),
            row.getPracticePaperId(),
            row.getChosenPracticeTopicId(),
            row.getTopicName(),
            row.getOrigin(),
            row.getStatus(),
            row.getAbandonDiagnosis(),
            row.getOverallScore(),
            row.getGradedSeconds(),
            row.getStartedAt().toString(),
            row.getEndedAt() == null ? null : row.getEndedAt().toString(),
            row.getPendingEvaluations()
        );
    }
}
