package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.infrastructure.persistence.entity.question.QuestionJpaEntity;

public final class QuestionMapper {

    public static Question toDomain(QuestionJpaEntity jpa) {
        return Question.builder()
                .id(jpa.getId())
                .questionBankId(jpa.getQuestionBankId())
                .questionTopicId(jpa.getQuestionTopicId())
                .code(jpa.getCode())
                .instructionText(jpa.getInstructionText())
                .questionText(jpa.getQuestionText())
                .promptText(jpa.getPromptText())
                .preparationText(jpa.getPreparationText())
                .type(QuestionType.valueOf(jpa.getType()))
                .preparationTimeSeconds(jpa.getPreparationTimeSeconds())
                .minResponseSeconds(jpa.getMinResponseSeconds())
                .maxResponseSeconds(jpa.getMaxResponseSeconds())
                .sharing(sharingFromString(jpa.getSharing()))
                .sourceQuestionId(jpa.getSourceQuestionId())
                .locked(jpa.isLocked())
                .confidentiality(QuestionConfidentiality.valueOf(jpa.getConfidentiality()))
                .securePoolId(jpa.getSecurePoolId())
                .status(QuestionStatus.valueOf(jpa.getStatus()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static QuestionJpaEntity toJpa(Question question) {
        return QuestionJpaEntity.builder()
                .id(question.getId())
                .questionBankId(question.getQuestionBankId())
                .questionTopicId(question.getQuestionTopicId())
                .code(question.getCode())
                .instructionText(question.getInstructionText())
                .questionText(question.getQuestionText())
                .promptText(question.getPromptText())
                .preparationText(question.getPreparationText())
                .type(question.getType().name())
                .preparationTimeSeconds(question.getPreparationTimeSeconds())
                .minResponseSeconds(question.getMinResponseSeconds())
                .maxResponseSeconds(question.getMaxResponseSeconds())
                .sharing(valueOf(question.getSharing()))
                .sourceQuestionId(question.getSourceQuestionId())
                .locked(question.isLocked())
                .confidentiality(question.getConfidentiality().name())
                .securePoolId(question.getSecurePoolId())
                .status(question.getStatus().name())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .createdBy(question.getCreatedBy())
                .updatedBy(question.getUpdatedBy())
                .build();
    }

    private static String valueOf(QuestionSharing sharing) {
        return sharing == null ? null : sharing.name();
    }

    private static QuestionSharing sharingFromString(String sharing) {
        return sharing == null ? null : QuestionSharing.valueOf(sharing);
    }

    
}
