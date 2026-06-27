package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.infrastructure.persistence.entity.QuestionJpaEntity;

public final class QuestionMapper {

    public static Question toDomain(QuestionJpaEntity jpa) {
        return new Question(
            jpa.getId(),
            jpa.getQuestionBankId(),
            jpa.getQuestionTopicId(),
            jpa.getCode(),
            jpa.getInstructionText(),
            jpa.getQuestionText(),
            jpa.getPromptText(),
            jpa.getPreparationText(),
            QuestionType.valueOf(jpa.getType()),
            jpa.getPreparationTimeSeconds(),
            jpa.getMinResponseSeconds(),
            jpa.getMaxResponseSeconds(),
            sharingFromString(jpa.getSharing()),
            jpa.getSourceQuestionId(),
            jpa.isLocked(),
            QuestionConfidentiality.valueOf(jpa.getConfidentiality()), 
            jpa.getSecurePoolId(),
            QuestionStatus.valueOf(jpa.getStatus()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static QuestionJpaEntity toJpa(Question question) {
        return new QuestionJpaEntity(
            question.getId(), 
            question.getQuestionBankId(),
            question.getQuestionTopicId(), 
            question.getCode(),
            question.getInstructionText(),
            question.getQuestionText(),
            question.getPromptText(),
            question.getPreparationText(),
            question.getType().name(),
            question.getPreparationTimeSeconds(),
            question.getMinResponseSeconds(),
            question.getMaxResponseSeconds(),
            valueOf(question.getSharing()),
            question.getSourceQuestionId(),
            question.isLocked(),
            question.getConfidentiality().name(), 
            question.getSecurePoolId(),
            question.getStatus().name(),
            question.getCreatedAt(),
            question.getUpdatedAt(),
            question.getCreatedBy(),
            question.getUpdatedBy()
        );
    }

    private static String valueOf(QuestionSharing sharing) {
        return sharing == null ? null : sharing.name();
    }

    private static QuestionSharing sharingFromString(String sharing) {
        return sharing == null ? null : QuestionSharing.valueOf(sharing);
    }

    
}
