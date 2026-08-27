package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "practice_topics",
    indexes = {
        @Index(
            name = "idx_practice_topic_normalized_name",
            columnList = "normalized_name",
            unique = true
        ),
        @Index(
            name = "idx_practice_topic_dimension_active",
            columnList = "interest_dimensions, active"
        )
    }
)
public class PracticeTopicJpaEntity {

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

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 200)
    private String normalizedName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source", nullable = false, length = 24)
    private String source;

    @Column(name = "interest_dimensions", nullable = false, length = 32)
    private String interestDimension;

    @Column(name = "curriculum_group", nullable = false, length = 24)
    private String curriculumGroup;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "source_question_topic_id", updatable = false)
    private UUID sourceQuestionTopicId;

    /** Xem {@code PracticeTopic.temporalAffordance}: PAST / FUTURE / MIXED, null coi như MIXED. */
    @Column(name = "temporal_affordance", length = 8)
    private String temporalAffordance;

    protected PracticeTopicJpaEntity() {
    }

    public PracticeTopicJpaEntity(
            String name,
            String normalizedName,
            String description,
            String source,
            String interestDimension,
            String curriculumGroup,
            boolean active,
            Instant createdAt,
            UUID sourceQuestionTopicId,
            String temporalAffordance) {
        this.temporalAffordance = temporalAffordance;
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = description;
        this.source = source;
        this.interestDimension = interestDimension;
        this.curriculumGroup = curriculumGroup;
        this.active = active;
        this.createdAt = createdAt;
        this.sourceQuestionTopicId = sourceQuestionTopicId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public String getDescription() {
        return description;
    }

    public String getSource() {
        return source;
    }

    public String getInterestDimension() {
        return interestDimension;
    }

    public String getCurriculumGroup() {
        return curriculumGroup;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getSourceQuestionTopicId() {
        return sourceQuestionTopicId;
    }

    public String getTemporalAffordance() {
        return temporalAffordance;
    }
}
