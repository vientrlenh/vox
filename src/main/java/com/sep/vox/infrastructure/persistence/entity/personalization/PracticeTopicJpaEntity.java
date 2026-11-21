package com.sep.vox.infrastructure.persistence.entity.personalization;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeTopicJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
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
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "source_question_topic_id", updatable = false)
    private UUID sourceQuestionTopicId;

    /** See {@code PracticeTopic.temporalAffordance}: PAST / FUTURE / MIXED, null counts as MIXED. */
    @Column(name = "temporal_affordance", length = 8)
    private String temporalAffordance;
}
