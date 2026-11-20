package com.sep.vox.infrastructure.persistence.query;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.NearestCentralizedAssessmentDto;
import com.sep.vox.application.query.repository.NearestCentralizedAssessmentQueryRepository;
import com.sep.vox.application.response.output.CandidateAssessmentProjection;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaNearestCentralizedAssessmentQueryRepository implements NearestCentralizedAssessmentQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<NearestCentralizedAssessmentDto> findNearestForSchool(UUID schoolId) {
        // The school's list of centralized assessments (usually only a few dozen) is read in full and the assessment
        // nearest to the current time is picked -- this avoids computing "|open_at - now()|" in JPQL,
        // which has no timestamp-subtraction function that is stable across Hibernate versions.
        List<CandidateAssessmentProjection> candidates = em.createQuery("""
            SELECT new com.sep.vox.application.response.output.CandidateAssessmentProjection(
                e.id, e.code, e.name, e.status, e.openAt, e.closeAt
            )
            FROM AssessmentJpaEntity e
            WHERE e.schoolId = :schoolId
              AND e.kind = 'CENTRALIZED'
              AND e.status <> 'CANCELLED'
              AND e.openAt IS NOT NULL
        """, CandidateAssessmentProjection.class)
            .setParameter("schoolId", schoolId)
            .getResultList();

        Instant now = Instant.now();
        Optional<CandidateAssessmentProjection> nearest = candidates.stream()
            .min(Comparator.comparing(c -> Duration.between(now, c.openAt()).abs()));

        if (nearest.isEmpty()) {
            return Optional.empty();
        }
        CandidateAssessmentProjection assessment = nearest.get();

        long totalCandidates = countCandidatesTotal(assessment.id());
        long absentCandidates = countCandidatesByStatus(assessment.id(), "ABSENT");

        return Optional.of(new NearestCentralizedAssessmentDto(
            assessment.id(),
            assessment.code(),
            assessment.name(),
            assessment.status(),
            assessment.openAt(),
            assessment.closeAt(),
            totalCandidates,
            absentCandidates
        ));
    }

    private long countCandidatesTotal(UUID assessmentId) {
        return em.createQuery("""
            SELECT COUNT(c)
            FROM AssessmentCandidateJpaEntity c
            WHERE c.assessmentId = :assessmentId
        """, Long.class)
            .setParameter("assessmentId", assessmentId)
            .getSingleResult();
    }

    private long countCandidatesByStatus(UUID assessmentId, String status) {
        return em.createQuery("""
            SELECT COUNT(c)
            FROM AssessmentCandidateJpaEntity c
            WHERE c.assessmentId = :assessmentId
              AND c.status = :status
        """, Long.class)
            .setParameter("assessmentId", assessmentId)
            .setParameter("status", status)
            .getSingleResult();
    }
}
