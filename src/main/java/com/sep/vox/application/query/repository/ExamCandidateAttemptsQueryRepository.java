package com.sep.vox.application.query.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.ExamAttemptSummary;

public interface ExamCandidateAttemptsQueryRepository {
    List<ExamAttemptSummary> findByCandidateIds(Collection<UUID> candidateIds);
}
