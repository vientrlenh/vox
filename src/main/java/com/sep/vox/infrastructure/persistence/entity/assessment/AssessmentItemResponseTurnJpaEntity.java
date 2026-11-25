package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "assessment_item_response_turns",
    indexes = {
        @Index(name = "idx_assessment_item_response_turns_response", columnList = "assessment_item_response_id"),
        @Index(name = "idx_assessment_item_response_turns_response_order", columnList = "assessment_item_response_id, turn_order")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_assessment_item_response_turns_response_order",
            columnNames = {"assessment_item_response_id", "turn_order"}
        )
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentItemResponseTurnJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "assessment_item_response_id", nullable = false, updatable = false)
    private UUID assessmentItemResponseId;

    @Column(name = "turn_order", nullable = false, updatable = false)
    private int turnOrder;

    @Column(name = "turn_type", length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_item_response_turns_turn_type_valid",
            constraint = "turn_type IN ('MAIN', 'FOLLOWUP')"
        )
    })
    private String turnType;

    @Column(name = "prompt_text", columnDefinition = "TEXT")
    private String promptText;

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
