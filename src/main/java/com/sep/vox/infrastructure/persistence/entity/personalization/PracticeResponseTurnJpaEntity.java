package com.sep.vox.infrastructure.persistence.entity.personalization;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "practice_response_turns",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_practice_response_turn_order",
        columnNames = {"practice_response_id", "turn_order"}
    ),
    indexes = @Index(name = "idx_practice_turn_response", columnList = "practice_response_id")
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeResponseTurnJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "practice_response_id", nullable = false, updatable = false)
    private UUID practiceResponseId;
    @Column(name = "turn_order", nullable = false, updatable = false)
    private int turnOrder;
    @Column(name = "turn_type", nullable = false, length = 24, updatable = false)
    private String turnType;
    @Column(name = "prompt_text", columnDefinition = "TEXT", updatable = false)
    private String promptText;
    @Column(name = "audio_url", columnDefinition = "TEXT", updatable = false)
    private String audioUrl;
    @Column(name = "transcript", columnDefinition = "TEXT", updatable = false)
    private String transcript;
    @Column(name = "duration_seconds", nullable = false, updatable = false)
    private int durationSeconds;
    @Column(name = "word_feedback_json", columnDefinition = "TEXT", updatable = false)
    private String wordFeedbackJson;
    @Column(name = "turn_score", precision = 5, scale = 2)
    private BigDecimal turnScore;
}
