package com.sep.vox.infrastructure.persistence.query;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.NearestCentralizedExamDto;
import com.sep.vox.application.query.repository.NearestCentralizedExamQueryRepository;
import com.sep.vox.application.response.output.CandidateExamProjection;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaNearestCentralizedExamQueryRepository implements NearestCentralizedExamQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<NearestCentralizedExamDto> findNearestForSchool(UUID schoolId) {
        // The school's list of centralized exams (usually only a few dozen) is read in full and the exam
        // nearest to the current time is picked -- this avoids computing "|open_at - now()|" in JPQL,
        // which has no timestamp-subtraction function that is stable across Hibernate versions.
        List<CandidateExamProjection> candidates = em.createQuery("""
            SELECT new com.sep.vox.application.response.output.CandidateExamProjection(
                e.id, e.code, e.name, e.status, e.openAt, e.closeAt
            )
            FROM ExamJpaEntity e
            WHERE e.schoolId = :schoolId
              AND e.kind = 'CENTRALIZED'
              AND e.status <> 'CANCELLED'
              AND e.openAt IS NOT NULL
        """, CandidateExamProjection.class)
            .setParameter("schoolId", schoolId)
            .getResultList();

        Instant now = Instant.now();
        Optional<CandidateExamProjection> nearest = candidates.stream()
            .min(Comparator.comparing(c -> Duration.between(now, c.openAt()).abs()));

        if (nearest.isEmpty()) {
            return Optional.empty();
        }
        CandidateExamProjection exam = nearest.get();

        long totalCandidates = countCandidatesTotal(exam.id());
        long absentCandidates = countCandidatesByStatus(exam.id(), "ABSENT");

        return Optional.of(new NearestCentralizedExamDto(
            exam.id(),
            exam.code(),
            exam.name(),
            exam.status(),
            exam.openAt(),
            exam.closeAt(),
            totalCandidates,
            absentCandidates
        ));
    }

    private long countCandidatesTotal(UUID examId) {
        return em.createQuery("""
            SELECT COUNT(c)
            FROM ExamCandidateJpaEntity c
            WHERE c.examId = :examId
        """, Long.class)
            .setParameter("examId", examId)
            .getSingleResult();
    }

    private long countCandidatesByStatus(UUID examId, String status) {
        return em.createQuery("""
            SELECT COUNT(c)
            FROM ExamCandidateJpaEntity c
            WHERE c.examId = :examId
              AND c.status = :status
        """, Long.class)
            .setParameter("examId", examId)
            .setParameter("status", status)
            .getSingleResult();
    }
}
