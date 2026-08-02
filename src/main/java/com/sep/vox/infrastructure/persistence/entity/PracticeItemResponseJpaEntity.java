package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "practice_item_response",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_practice_response_session_question",
        columnNames = {"practice_session_id", "practice_question_id"}
    ),
    indexes = @Index(name = "idx_practice_response_session", columnList = "practice_session_id")
)
public class PracticeItemResponseJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "practice_session_id", nullable = false, updatable = false)
    private UUID practiceSessionId;
    @Column(name = "practice_question_id", nullable = false, updatable = false)
    private UUID practiceQuestionId;
    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;
    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    protected PracticeItemResponseJpaEntity() {
    }

    public PracticeItemResponseJpaEntity(
            UUID id,
            UUID practiceSessionId,
            UUID practiceQuestionId,
            String audioUrl,
            String transcript) {
        this.id = id;
        this.practiceSessionId = practiceSessionId;
        this.practiceQuestionId = practiceQuestionId;
        this.audioUrl = audioUrl;
        this.transcript = transcript;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPracticeSessionId() {
        return practiceSessionId;
    }

    public UUID getPracticeQuestionId() {
        return practiceQuestionId;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }
}
