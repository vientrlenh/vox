package com.sep.vox.infrastructure.persistence.query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.ProctorCandidateSummary;
import com.sep.vox.application.query.repository.ProctorScheduleCandidatesQueryRepository;
import com.sep.vox.infrastructure.persistence.entity.ExamSessionJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaProctorScheduleCandidatesQueryRepository implements ProctorScheduleCandidatesQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ProctorCandidateSummary> findByScheduleId(UUID scheduleId) {
        var rows = em.createQuery("""
            SELECT candidate.id, user.id, user.fullName, user.email, candidate.status, candidate.blockedAt
            FROM ExamCandidateJpaEntity candidate
            JOIN UserJpaEntity user ON user.id = candidate.studentId
            WHERE candidate.scheduleId = :scheduleId
            ORDER BY user.fullName ASC
        """, Object[].class)
            .setParameter("scheduleId", scheduleId)
            .getResultList();

        if (rows.isEmpty()) {
            return List.of();
        }

        var candidateIds = rows.stream().map(row -> (UUID) row[0]).toList();

        // Sessions of every candidate in this schedule, most recent first --
        // used below to keep only the LATEST session per candidateId so proctors
        // see live exam-session status (not just attendance status).
        var sessions = em.createQuery("""
            SELECT s FROM ExamSessionJpaEntity s
            WHERE s.candidateId IN :candidateIds
            ORDER BY s.startedAt DESC
        """, ExamSessionJpaEntity.class)
            .setParameter("candidateIds", candidateIds)
            .getResultList();

        Map<UUID, ExamSessionJpaEntity> latestSessionByCandidateId = new LinkedHashMap<>();
        for (var session : sessions) {
            latestSessionByCandidateId.putIfAbsent(session.getCandidateId(), session);
        }

        return rows.stream()
            .map(row -> {
                var candidateId = (UUID) row[0];
                var latestSession = latestSessionByCandidateId.get(candidateId);
                return new ProctorCandidateSummary(
                    candidateId,
                    (UUID) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (java.time.OffsetDateTime) row[5],
                    latestSession == null ? null : latestSession.getId(),
                    latestSession == null ? null : latestSession.getStatus(),
                    latestSession != null && latestSession.isFlagged()
                );
            })
            .toList();
    }
}
