package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.valueobject.DifficultyLevel;
import com.sep.vox.domain.valueobject.QuestionType;
import com.sep.vox.infrastructure.persistence.entity.QuestionJpaEntity;

public final class QuestionMapper {

    public static Question toDomain(QuestionJpaEntity jpa) {
        return new Question(
            jpa.getId(),
            jpa.getTopicId(),
            jpa.getQuestionText(),
            jpa.getAudioUrl(),
            new DifficultyLevel(jpa.getDifficultyLevel()),
            new QuestionType(jpa.getQuestionType()),
            jpa.getDurationSeconds(),
            jpa.isActive(),
            jpa.getCreatedAt()
        );
    }

    public static QuestionJpaEntity toJpa(Question domain) {
        return new QuestionJpaEntity(
            domain.getId(),
            domain.getTopicId(),
            domain.getQuestionText(),
            domain.getAudioUrl(),
            domain.getDifficultyLevel().value(),
            domain.getQuestionType().value(),
            domain.getDurationSeconds(),
            domain.isActive(),
            domain.getCreatedAt()
        );
    }
}
