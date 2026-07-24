package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamItemResponseTurn;
import com.sep.vox.domain.model.exam.TurnType;
import com.sep.vox.infrastructure.persistence.entity.ExamItemResponseTurnJpaEntity;

public final class ExamItemResponseTurnMapper {

    private ExamItemResponseTurnMapper() {
    }

    public static ExamItemResponseTurn toDomain(ExamItemResponseTurnJpaEntity jpa) {
        return new ExamItemResponseTurn(
            jpa.getId(),
            jpa.getExamItemResponseId(),
            jpa.getTurnOrder(),
            TurnType.valueOf(jpa.getTurnType()),
            jpa.getPromptText(),
            jpa.getAudioUrl(),
            jpa.getTranscript(),
            jpa.getDurationSeconds(),
            jpa.getWordCount(),
            jpa.getAnsweredAt(),
            jpa.getCreatedAt()
        );
    }

    public static ExamItemResponseTurnJpaEntity toJpa(ExamItemResponseTurn domain) {
        return new ExamItemResponseTurnJpaEntity(
            domain.getId(),
            domain.getExamItemResponseId(),
            domain.getTurnOrder(),
            domain.getTurnType().name(),
            domain.getPromptText(),
            domain.getAudioUrl(),
            domain.getTranscript(),
            domain.getDurationSeconds(),
            domain.getWordCount(),
            domain.getAnsweredAt(),
            domain.getCreatedAt()
        );
    }
}
