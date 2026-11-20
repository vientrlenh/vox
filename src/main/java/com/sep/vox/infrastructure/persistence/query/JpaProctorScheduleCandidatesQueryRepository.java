package com.sep.vox.infrastructure.persistence.query;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.ProctorCandidateSummary;
import com.sep.vox.application.query.repository.ProctorScheduleCandidatesQueryRepository;
import com.sep.vox.infrastructure.persistence.entity.AssessmentSessionJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaProctorScheduleCandidatesQueryRepository implements ProctorScheduleCandidatesQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ProctorCandidateSummary> findByScheduleId(UUID scheduleId) {
        List<Object[]> rows = em.createQuery("""
            SELECT candidate.id, user.id, user.fullName, user.email, candidate.status, candidate.blockedAt
            FROM AssessmentCandidateJpaEntity candidate
            JOIN UserJpaEntity user ON user.id = candidate.studentId
            WHERE candidate.scheduleId = :scheduleId
            ORDER BY user.fullName ASC
        """, Object[].class)
            .setParameter("scheduleId", scheduleId)
            .getResultList();

        if (rows.isEmpty()) {
            return List.of();
        }

        List<UUID> candidateIds = rows.stream().map(row -> (UUID) row[0]).toList();

        // Sessions of every candidate in this schedule, most recent first --
        // used below to keep only the LATEST session per candidateId so proctors
        // see live assessment-session status (not just attendance status).
        List<AssessmentSessionJpaEntity> sessions = em.createQuery("""
            SELECT s FROM AssessmentSessionJpaEntity s
            WHERE s.candidateId IN :candidateIds
            ORDER BY s.startedAt DESC
        """, AssessmentSessionJpaEntity.class)
            .setParameter("candidateIds", candidateIds)
            .getResultList();

        Map<UUID, AssessmentSessionJpaEntity> latestSessionByCandidateId = new LinkedHashMap<>();
        for (AssessmentSessionJpaEntity session : sessions) {
            latestSessionByCandidateId.putIfAbsent(session.getCandidateId(), session);
        }

        return rows.stream()
            .map(row -> {
                UUID candidateId = (UUID) row[0];
                AssessmentSessionJpaEntity latestSession = latestSessionByCandidateId.get(candidateId);
                return new ProctorCandidateSummary(
                    candidateId,
                    (UUID) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (Instant) row[5],
                    latestSession == null ? null : latestSession.getId(),
                    latestSession == null ? null : latestSession.getStatus(),
                    latestSession != null && latestSession.isFlagged()
                );
            })
            .toList();
    }
}
