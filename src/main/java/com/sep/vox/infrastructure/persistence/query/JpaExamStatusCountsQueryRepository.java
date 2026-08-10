package com.sep.vox.infrastructure.persistence.query;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.ExamStatusCountsDto;
import com.sep.vox.application.query.repository.ExamStatusCountsQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaExamStatusCountsQueryRepository implements ExamStatusCountsQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public ExamStatusCountsDto countAccessibleByStatus(
            UUID currentUserId,
            UUID currentSchoolId,
            boolean systemAdmin,
            boolean schoolAdmin,
            UUID schoolId,
            String kind,
            Instant createdFrom,
            Instant createdTo) {
        var jpql = new StringBuilder("""
            SELECT new com.sep.vox.application.query.dto.ExamStatusCountsDto(
                COUNT(e),
                COALESCE(SUM(CASE WHEN e.status = 'DRAFT' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN e.status = 'SCHEDULED' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN e.status = 'IN_PROGRESS' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN e.status = 'CLOSED' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN e.status = 'RESULTS_PUBLISHED' THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN e.status = 'CANCELLED' THEN 1L ELSE 0L END), 0L)
            )
            FROM ExamJpaEntity e
            WHERE (:schoolId IS NULL OR e.schoolId = :schoolId)
              AND (:kind IS NULL OR e.kind = :kind)
            """);
        if (createdFrom != null) {
            jpql.append(" AND e.createdAt >= :createdFrom\n");
        }
        if (createdTo != null) {
            jpql.append(" AND e.createdAt <= :createdTo\n");
        }
        jpql.append("""
              AND (
                    :systemAdmin = true
                    OR (:schoolAdmin = true AND e.schoolId = :currentSchoolId)
                    OR EXISTS (
                        SELECT 1
                        FROM ExamMemberJpaEntity em
                        WHERE em.examId = e.id
                          AND em.userId = :currentUserId
                    )
                  )
            """);

        var query = em.createQuery(jpql.toString(), ExamStatusCountsDto.class)
            .setParameter("schoolId", schoolId)
            .setParameter("kind", kind)
            .setParameter("systemAdmin", systemAdmin)
            .setParameter("schoolAdmin", schoolAdmin)
            .setParameter("currentSchoolId", currentSchoolId)
            .setParameter("currentUserId", currentUserId);
        if (createdFrom != null) {
            query.setParameter("createdFrom", createdFrom);
        }
        if (createdTo != null) {
            query.setParameter("createdTo", createdTo);
        }
        return query.getSingleResult();
    }
}
