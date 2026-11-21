package com.sep.vox.infrastructure.persistence.entity.personalization;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "topic_suggestions",
    indexes = {
        @Index(name = "idx_topic_suggestion_student_status", columnList = "student_id, status"),
        @Index(name = "idx_topic_suggestion_student_name", columnList = "student_id, suggested_topic_name")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopicSuggestionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;
    @Column(name = "suggested_topic_name", nullable = false, length = 200, updatable = false)
    private String suggestedTopicName;
    @Column(name = "keyword", length = 200, updatable = false)
    private String keyword;
    @Column(name = "interest_dimensions", nullable = false, length = 32, updatable = false)
    private String interestDimension;
    @Column(name = "curriculum_group", length = 24, updatable = false)
    private String curriculumGroup;
    @Column(name = "confidence", nullable = false, precision = 4, scale = 3, updatable = false)
    private BigDecimal confidence;
    @Column(name = "reason_text", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String reasonText;
    @Column(name = "evidence_json", columnDefinition = "TEXT", updatable = false)
    private String evidenceJson;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "responded_at")
    private Instant respondedAt;
}
