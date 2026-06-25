package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamCandidateResult {
    private UUID id;
    private UUID examId;
    private UUID candidateId;
    private UUID sessionId;

    private UUID assessmentPolicyId;
    private int policyVersion;
    private UUID rubricVersionId;
    private UUID frameworkVersionId;
    private UUID targetFrameworkBandId;

    private ExamCandidateResultStatus status;
    private OffsetDateTime releasedAt;
    private OffsetDateTime finalizedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
