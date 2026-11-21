package com.sep.vox.infrastructure.persistence.entity.personalization;

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
    name = "turn_corrections",
    indexes = @Index(name = "idx_turn_correction_turn", columnList = "turn_id")
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TurnCorrectionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "turn_id", nullable = false, updatable = false)
    private UUID turnId;
    @Column(name = "category", nullable = false, length = 64, updatable = false)
    private String category;
    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String originalText;
    @Column(name = "corrected_text", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String correctedText;
    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String explanation;
    @Column(name = "correct_audio_url", columnDefinition = "TEXT")
    private String correctAudioUrl;
}
