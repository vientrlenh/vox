package com.sep.vox.application.query.dto;

import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import java.time.Instant;
import java.util.UUID;

public record ProctorCandidateSummary(
    UUID candidateId,
    UUID studentId,
    String studentName,
    String studentEmail,
    AssessmentCandidateStatus status,
    Instant blockedAt,
    UUID sessionId,
    AssessmentSessionStatus sessionStatus,
    boolean sessionFlagged
) {
    // Auxiliary constructor for the query repository, which reads both statuses as plain String columns of the JPA entities: it
    // takes the raw strings and converts them here, keeping the record itself enum-typed.
    public ProctorCandidateSummary(
            UUID candidateId,
            UUID studentId,
            String studentName,
            String studentEmail,
            String status,
            Instant blockedAt,
            UUID sessionId,
            String sessionStatus,
            boolean sessionFlagged) {
        this(candidateId, studentId, studentName, studentEmail,
            status == null ? null : AssessmentCandidateStatus.valueOf(status),
            blockedAt, sessionId,
            sessionStatus == null ? null : AssessmentSessionStatus.valueOf(sessionStatus),
            sessionFlagged);
    }
}
