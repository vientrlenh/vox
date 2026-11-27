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

    /**
     * Marks the session as STILL ALIVE -- sets {@code last_heartbeat_at} to now.
     *
     * <p>Before 2026-08-12 this column was written EXACTLY ONCE when the session was created and never updated anywhere
     * (the setter existed but nobody called it). Yet PracticeSessionHeartbeatCleanupJob closes every session
     * whose heartbeat is older than the threshold, and the job runs every 30 seconds -- so EVERY practice session was closed at around
     * the 3rd minute, regardless of whether the student was speaking. After that every turn submission returned 404 "Practice session is no
     * longer active" (measured on production: session 681cdd72 opened 09:46:48, turn submitted 09:52:11 -> 404).
     *
     * <p>Only UPDATEs one column instead of loading the entity and saving: it runs on the hot realtime path, and
     * must not overwrite the columns that the grading flow is updating in parallel.
     */
    void touchHeartbeat(UUID sessionId);
}
