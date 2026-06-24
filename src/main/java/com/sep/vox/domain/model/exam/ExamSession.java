package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamSession {
    private UUID id;
    private UUID examId;
    private UUID candidateId;
    private UUID paperId;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;
    private ExamSessionStatus status; 

    public ExamSession() {}

    public ExamSession(UUID id, UUID examId, UUID candidateId, UUID paperId, OffsetDateTime startedAt,
            OffsetDateTime submittedAt, ExamSessionStatus status) {
        this.id = id;
        this.examId = examId;
        this.candidateId = candidateId;
        this.paperId = paperId;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.status = status;
    }

    public ExamSession(UUID examId, UUID candidateId, UUID paperId, OffsetDateTime startedAt,
            OffsetDateTime submittedAt, ExamSessionStatus status) {
        this.examId = examId;
        this.candidateId = candidateId;
        this.paperId = paperId;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getExamId() {
        return examId;
    }

    public void setExamId(UUID examId) {
        this.examId = examId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public UUID getPaperId() {
        return paperId;
    }

    public void setPaperId(UUID paperId) {
        this.paperId = paperId;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public ExamSessionStatus getStatus() {
        return status;
    }

    public void setStatus(ExamSessionStatus status) {
        this.status = status;
    }

    
}
