package com.sep.vox.infrastructure.persistence.entity.question;

import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "questions", indexes = {
    @Index(columnList = "question_topic_id, code", name = "idx_questions_question_topic_code", unique = true),
    @Index(columnList = "type", name = "idx_questions_type"),
    @Index(columnList = "sharing", name = "idx_questions_sharing"),
    @Index(columnList = "source_question_id", name = "idx_questions_source_question")
}, check = {
    @CheckConstraint(
        name = "chk_questions_min_response_seconds_and_max_response_seconds_valid", 
        constraint = "min_response_seconds <= max_response_seconds"
    )
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionJpaEntity extends BaseEntity {

    @Column(name = "question_bank_id", nullable = false, updatable = false)
    private UUID questionBankId;

    @Column(name = "question_topic_id", updatable = false)
    private UUID questionTopicId;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "instruction_text", columnDefinition = "TEXT")
    private String instructionText;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "prompt_text", columnDefinition = "TEXT")
    private String promptText;

    @Column(name = "preparation_text", columnDefinition = "TEXT")
    private String preparationText;

    @Column(name = "type", nullable = false, length = 50, check = {
        @CheckConstraint(
            name = "chk_questions_type_valid",
            constraint = "type IN ('READ_ALOUD', 'SHORT_ANSWER', 'LONG_ANSWER', 'OPINION', 'DESCRIPTION')"
        )
    })
    private String type;

    @Column(name = "preparation_time_seconds", nullable = false, check = {
        @CheckConstraint(
            name = "chk_questions_preparation_time_seconds_valid", 
            constraint = "preparation_time_seconds >= 0"
        )
    })
    private int preparationTimeSeconds;

    @Column(name = "min_response_seconds", nullable = false, check = {
        @CheckConstraint(
            name = "chk_questions_min_response_seconds_valid", 
            constraint = "min_response_seconds >= 0"
        )
    })
    private int minResponseSeconds;

    @Column(name = "max_response_seconds", nullable = false, check = {
        @CheckConstraint(
            name = "chk_questions_max_response_seconds_valid", 
            constraint = "max_response_seconds >= 0"
        )
    })
    private int maxResponseSeconds;

    @Column(name = "sharing", length = 100, check = {
        @CheckConstraint(
            name = "chk_questions_sharing_valid", 
            constraint = "sharing IN ('PRIVATE', 'SCHOOL_SHARED')"
        )
    })
    private String sharing;

    @Column(name = "source_question_id")
    private UUID sourceQuestionId;

    @Column(name = "locked", nullable = false)
    private boolean locked;

    @Column(name = "confidentiality", nullable = false, length = 32, check = {
        @CheckConstraint(
            name = "chk_questions_confidentiality_valid", 
            constraint = "confidentiality IN ('OPEN', 'ASSESSMENT_RESTRICTED', 'RELEASED')"
        )
    })
    private String confidentiality;

    @Column(name = "secure_pool_id")
    private UUID securePoolId;

    @Column(name = "status", nullable = false, check = {
        @CheckConstraint(
            name = "chk_status_valid", 
            constraint = "status IN ('DRAFT', 'SUBMITTED_FOR_REVIEW', 'REVISION_REQUESTED', 'APPROVED', 'REJECTED', 'PUBLISHED', 'ARCHIVED')"
        )
    })
    private String status;
}
