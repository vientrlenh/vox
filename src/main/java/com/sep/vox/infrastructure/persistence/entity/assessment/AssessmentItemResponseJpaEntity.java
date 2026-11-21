package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_item_responses")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentItemResponseJpaEntity {

    @Id
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false
    )
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId; 

    @Column(name = "paper_item_id")
    private UUID paperItemId;

    @Column(name = "audio_url", length = 4096)
    private String audioUrl; 

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    // TEXT, not a short varchar: the value is a free-text reason from the AI's follow-up decision
    // (e.g. "The student has only provided one reason for enjoying reading and has not
    // elaborated..."), not a short status code -- length = 100 truncated nothing, it just made
    // every save fail with "value too long" whenever the reason ran past 100 characters, which is
    // the common case, not an edge case. Matches the transcript column's pattern above.
    @Column(name = "termination_reason", columnDefinition = "TEXT")
    private String terminationReason;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;
}
