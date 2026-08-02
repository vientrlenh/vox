package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "practice_question",
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
            constraint = "difficulty_rank BETWEEN 1 AND 6"
        ),
        @CheckConstraint(
            name = "chk_practice_question_time_budgets",
            constraint = "preparation_time_seconds >= 0 AND max_response_seconds > 0 AND max_followup_seconds >= 0"
        )
    }
)
public class PracticeQuestionJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "practice_topic_id", nullable = false, updatable = false)
    private UUID practiceTopicId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "target_criterion_code", nullable = false, length = 32)
    private String targetCriterionCode;

    @Column(name = "target_sub_attribute", length = 64)
    private String targetSubAttribute;

    @Column(name = "difficulty_rank", nullable = false)
    private int difficultyRank;

    @Column(name = "difficulty_features_json", nullable = false, columnDefinition = "TEXT")
    private String difficultyFeaturesJson;

    @Column(name = "evaluation_guide_json", nullable = false, columnDefinition = "TEXT")
    private String evaluationGuideJson;

    @Column(name = "suggested_ideas_json", columnDefinition = "TEXT")
    private String suggestedIdeasJson;

    @Column(name = "preparation_time_seconds", nullable = false)
    private int preparationTimeSeconds;

    @Column(name = "max_response_seconds", nullable = false)
    private int maxResponseSeconds;

    @Column(name = "max_followup_seconds", nullable = false)
    private int maxFollowupSeconds;

    @Column(name = "vstep_part")
    private Integer vstepPart;

    @Column(name = "source", nullable = false, length = 24)
    private String source = "AI_GENERATED";

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PracticeQuestionJpaEntity() {
    }

    public PracticeQuestionJpaEntity(
            UUID practiceTopicId,
            String questionText,
            String targetCriterionCode,
            String targetSubAttribute,
            int difficultyRank,
            String difficultyFeaturesJson,
            String evaluationGuideJson,
            String suggestedIdeasJson,
            int preparationTimeSeconds,
            int maxResponseSeconds,
            int maxFollowupSeconds,
            Integer vstepPart,
            String source,
            int usageCount,
            boolean active,
            OffsetDateTime createdAt) {
        this.practiceTopicId = practiceTopicId;
        this.questionText = questionText;
        this.targetCriterionCode = targetCriterionCode;
        this.targetSubAttribute = targetSubAttribute;
        this.difficultyRank = difficultyRank;
        this.difficultyFeaturesJson = difficultyFeaturesJson;
        this.evaluationGuideJson = evaluationGuideJson;
        this.suggestedIdeasJson = suggestedIdeasJson;
        this.preparationTimeSeconds = preparationTimeSeconds;
        this.maxResponseSeconds = maxResponseSeconds;
        this.maxFollowupSeconds = maxFollowupSeconds;
        this.vstepPart = vstepPart;
        this.source = source;
        this.usageCount = usageCount;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPracticeTopicId() {
        return practiceTopicId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getTargetCriterionCode() {
        return targetCriterionCode;
    }

    public String getTargetSubAttribute() {
        return targetSubAttribute;
    }

    public int getDifficultyRank() {
        return difficultyRank;
    }

    public String getDifficultyFeaturesJson() {
        return difficultyFeaturesJson;
    }

    public String getEvaluationGuideJson() {
        return evaluationGuideJson;
    }

    public String getSuggestedIdeasJson() {
        return suggestedIdeasJson;
    }

    public int getPreparationTimeSeconds() {
        return preparationTimeSeconds;
    }

    public int getMaxResponseSeconds() {
        return maxResponseSeconds;
    }

    public int getMaxFollowupSeconds() {
        return maxFollowupSeconds;
    }

    public Integer getVstepPart() {
        return vstepPart;
    }

    public String getSource() {
        return source;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
