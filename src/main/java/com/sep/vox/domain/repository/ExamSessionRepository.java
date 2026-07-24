package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;

public interface ExamSessionRepository {
    Optional<ExamSession> findById(UUID id);
    Optional<ExamSession> findLatestByExamIdAndCandidateId(UUID examId, UUID candidateId);
    Optional<ExamSession> findLatestByCandidateId(UUID candidateId);
    Optional<ExamSession> findLatestByCandidateIdAndStatuses(UUID candidateId, Collection<ExamSessionStatus> statuses);
    List<ExamSession> findAllByCandidateId(UUID candidateId);
    List<ExamSession> findAllByCandidateIdIn(Collection<UUID> candidateIds);
    List<ExamSession> findDeferredGradingCandidates(java.time.OffsetDateTime now);
    List<ExamSession> findPastScheduleEndCandidates(java.time.OffsetDateTime threshold);
    boolean existsById(UUID id);
    ExamSession save(ExamSession session);
    void deleteById(UUID id);
    Optional<ExamSession> findByIdAndInProgress(UUID id);
    Optional<ExamSession> findActiveByExamIdAndCandidateId(UUID examId, UUID candidateId);
    List<ExamSession> findActiveByIdInAndSchoolId(Collection<UUID> ids, OffsetDateTime now, UUID schoolId);
}
