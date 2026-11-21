package com.sep.vox.infrastructure.persistence.entity.personalization;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "practice_questions",
    indexes = {
        @Index(
            name = "idx_practice_question_lookup",
            columnList = "practice_topic_id, target_criterion_code, difficulty_rank, active"
        ),
        @Index(
            name = "idx_practice_question_sub_attribute",
            columnList = "target_sub_attribute"
        )
    },
    check = {
        @CheckConstraint(
            name = "chk_practice_question_difficulty_rank",
            constraint = "difficulty_rank BETWEEN 1 AND 20"
        ),
        @CheckConstraint(
            name = "chk_practice_question_time_budgets",
            constraint = "min_response_seconds > 0 "
                + "AND max_response_seconds > min_response_seconds"
        )
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeQuestionJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "practice_topic_id", nullable = false, updatable = false)
    private UUID practiceTopicId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "target_criterion_code", nullable = false, length = 32)
    private String targetCriterionCode;

    @Column(name = "target_sub_attribute", length = 64)
    private String targetSubAttribute;

    /** See {@code PracticeQuestion.targetTense}: null = unknown, not "no tense is forced". */
    @Column(name = "target_tense", length = 16)
    private String targetTense;

    @Column(name = "difficulty_rank", nullable = false)
    private int difficultyRank;

    @Column(name = "difficulty_features_json", nullable = false, columnDefinition = "TEXT")
    private String difficultyFeaturesJson;

    @Column(name = "evaluation_guide_json", nullable = false, columnDefinition = "TEXT")
    private String evaluationGuideJson;

    @Column(name = "suggested_ideas_json", columnDefinition = "TEXT")
    private String suggestedIdeasJson;

    @Column(name = "question_type", nullable = false, length = 24)
    private String questionType;

    @Column(name = "max_response_seconds", nullable = false)
    private int maxResponseSeconds;

    @Column(name = "min_response_seconds", nullable = false)
    private int minResponseSeconds;

    @Column(name = "vstep_part")
    private Integer vstepPart;

    @Column(name = "source", nullable = false, length = 24)
    @Builder.Default
    private String source = "AI_GENERATED";

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
