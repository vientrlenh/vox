package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.PracticeSession;

public interface PracticeSessionRepository {

    Optional<PracticeSession> findById(UUID id);

    Optional<PracticeSession> findByIdForUpdate(UUID id);

    boolean existsByIdAndStudentIdAndStatus(UUID id, UUID studentId, String status);

    PracticeSession save(PracticeSession session);

    List<PracticeSession> findStaleInProgress(Instant staleBefore);

    void refreshOverallScore(UUID sessionId);
}
