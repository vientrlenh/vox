package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.infrastructure.persistence.entity.QuestionJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.QuestionReadDtoMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class JpaQuestionReadQueryRepository implements QuestionReadQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<QuestionDto> findVisibleQuestion(UUID questionId, UUID userId, String role, UUID schoolId) {
        QuestionJpaEntity question;
        try {
            question = em.createQuery("""
                SELECT q FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'
                  AND (
                    :role = 'SYSTEM_ADMIN'
                    OR (:role = 'TEACHER' AND (
                        (qt.status = 'PUBLISHED' AND qb.status = 'PUBLISHED' AND (
                            (qb.ownerType = 'SYSTEM' OR (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId))
                            AND (
                            (q.visibility = 'BANK_VISIBLE' AND q.status = 'PUBLISHED')
                            OR (q.visibility = 'BANK_VISIBLE' AND q.status IN ('DRAFT','SUBMITTED_FOR_REVIEW','REVISION_REQUESTED','APPROVED','REJECTED') AND q.createdBy = :userId)
                            OR (q.visibility = 'AUTHOR_ONLY' AND q.createdBy = :userId)
                            OR (q.visibility = 'REVIEWER_ONLY' AND q.createdBy <> :userId AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId AND q.status = 'SUBMITTED_FOR_REVIEW')
                            )
                        ))
                        OR (qb.status = 'DRAFT' AND qt.status <> 'ARCHIVED' AND (
                            q.createdBy = :userId
                            OR (q.visibility = 'REVIEWER_ONLY' AND q.createdBy <> :userId AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId AND q.status = 'SUBMITTED_FOR_REVIEW')
                        ))
                    ))
                    OR (:role = 'SCHOOL_ADMIN' AND (
                        (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId AND qt.status <> 'ARCHIVED' AND q.status <> 'ARCHIVED')
                        OR (qb.ownerType = 'SYSTEM' AND qb.status = 'PUBLISHED' AND qt.status = 'PUBLISHED' AND q.status = 'PUBLISHED' AND q.visibility = 'BANK_VISIBLE')
                    ))
                  )
                """, QuestionJpaEntity.class)
                .setParameter("questionId", questionId)
                .setParameter("userId", userId)
                .setParameter("role", role)
                .setParameter("schoolId", schoolId)
                .getSingleResult();
        } catch (NoResultException e) {
            return Optional.empty();
        }
        return Optional.of(QuestionReadDtoMapper.toDto(question));
    }

    @Override
    public PageResult<QuestionDto> findTeacherMyQuestions(UUID userId, PageRequest page) {
        String countSql = "SELECT COUNT(q) FROM QuestionJpaEntity q WHERE q.createdBy = :userId";
        String dataSql = "SELECT q FROM QuestionJpaEntity q WHERE q.createdBy = :userId ORDER BY q.updatedAt DESC";

        Long total = em.createQuery(countSql, Long.class)
                .setParameter("userId", userId)
                .getSingleResult();

        List<QuestionJpaEntity> results = em.createQuery(dataSql, QuestionJpaEntity.class)
                .setParameter("userId", userId)
                .setFirstResult((page.page() - 1) * page.size())
                .setMaxResults(page.size())
                .getResultList();

        return QuestionReadDtoMapper.toDtoPage(results, total, page);
    }

    @Override
    public PageResult<QuestionDto> findTeacherVisibleQuestions(UUID userId, UUID schoolId, String scope, String status, String type, String keyword, PageRequest page) {
        StringBuilder where = new StringBuilder("""
            WHERE qb.status <> 'ARCHIVED'
              AND qt.status <> 'ARCHIVED'
              AND (
                (
                  qb.status = 'PUBLISHED'
                  AND qt.status = 'PUBLISHED'
                  AND (
                    qb.ownerType = 'SYSTEM'
                    OR (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)
                  )
                  AND (
                    (q.visibility = 'BANK_VISIBLE' AND q.status = 'PUBLISHED')
                    OR (q.visibility = 'BANK_VISIBLE' AND q.status IN ('DRAFT','SUBMITTED_FOR_REVIEW','REVISION_REQUESTED','APPROVED','REJECTED') AND q.createdBy = :userId)
                    OR (q.visibility = 'AUTHOR_ONLY' AND q.createdBy = :userId)
                    OR (q.visibility = 'REVIEWER_ONLY' AND q.createdBy <> :userId AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId AND q.status = 'SUBMITTED_FOR_REVIEW')
                  )
                )
                OR (
                  qb.ownerType = 'SCHOOL'
                  AND qb.schoolId = :schoolId
                  AND qb.status = 'DRAFT'
                  AND (
                    q.createdBy = :userId
                    OR (q.visibility = 'REVIEWER_ONLY' AND q.createdBy <> :userId AND q.status = 'SUBMITTED_FOR_REVIEW')
                  )
                )
              )
            """);

        appendQuestionFilters(where, scope, status, type, keyword);
        return findQuestionsWithJoinAndFilters(where.toString(), userId, schoolId, scope, status, type, keyword, page);
    }

    @Override
    public PageResult<QuestionDto> findTeacherReviewQueue(UUID userId, UUID schoolId, PageRequest page) {
        String where = """
            WHERE q.status = 'SUBMITTED_FOR_REVIEW'
              AND q.visibility = 'REVIEWER_ONLY'
              AND q.createdBy <> :userId
              AND qb.ownerType = 'SCHOOL'
              AND qb.schoolId = :schoolId
            """;

        return findQuestionsWithJoin(where, userId, schoolId, page);
    }

    @Override
    public PageResult<QuestionDto> findSchoolVisibleQuestions(UUID schoolId, String scope, String status, String type, String keyword, PageRequest page) {
        StringBuilder where = new StringBuilder("""
            WHERE qb.status <> 'ARCHIVED'
              AND qt.status <> 'ARCHIVED'
              AND (
                (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId AND q.status <> 'ARCHIVED')
                OR (qb.ownerType = 'SYSTEM' AND qb.status = 'PUBLISHED' AND qt.status = 'PUBLISHED' AND q.status = 'PUBLISHED' AND q.visibility = 'BANK_VISIBLE')
              )
            """);

        appendQuestionFilters(where, scope, status, type, keyword);
        return findQuestionsWithJoinAndFilters(where.toString(), null, schoolId, scope, status, type, keyword, page);
    }

    @Override
    public PageResult<QuestionDto> findSchoolReviewQueue(UUID schoolId, PageRequest page) {
        String where = """
            WHERE q.status = 'SUBMITTED_FOR_REVIEW'
              AND q.visibility = 'REVIEWER_ONLY'
              AND qb.ownerType = 'SCHOOL'
              AND qb.schoolId = :schoolId
            """;

        return findQuestionsWithJoin(where, null, schoolId, page);
    }

    @Override
    public PageResult<QuestionDto> findAdminQuestions(Boolean includeArchived, String status, String keyword, PageRequest page) {
        StringBuilder where = new StringBuilder("WHERE 1=1");
        if (!Boolean.TRUE.equals(includeArchived)) {
            where.append(" AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'");
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND q.status = :status");
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(q.questionText) LIKE :keyword OR LOWER(q.code) LIKE :keyword)");
        }

        String countSql = "SELECT COUNT(q) FROM QuestionJpaEntity q JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id " + where;
        String dataSql = "SELECT q FROM QuestionJpaEntity q JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id " + where + " ORDER BY q.updatedAt DESC";

        TypedQuery<Long> countQuery = em.createQuery(countSql, Long.class);
        TypedQuery<QuestionJpaEntity> dataQuery = em.createQuery(dataSql, QuestionJpaEntity.class);

        if (status != null && !status.isBlank()) {
            countQuery.setParameter("status", status);
            dataQuery.setParameter("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.toLowerCase() + "%";
            countQuery.setParameter("keyword", kw);
            dataQuery.setParameter("keyword", kw);
        }

        Long total = countQuery.getSingleResult();
        List<QuestionJpaEntity> results = dataQuery
                .setFirstResult((page.page() - 1) * page.size())
                .setMaxResults(page.size())
                .getResultList();

        return QuestionReadDtoMapper.toDtoPage(results, total, page);
    }

    @Override
    public PageResult<QuestionDto> findAdminReviewQueue(PageRequest page) {
        String where = "WHERE q.status = 'SUBMITTED_FOR_REVIEW'";
        String countSql = "SELECT COUNT(q) FROM QuestionJpaEntity q " + where;
        String dataSql = "SELECT q FROM QuestionJpaEntity q " + where + " ORDER BY q.updatedAt DESC";

        Long total = em.createQuery(countSql, Long.class).getSingleResult();
        List<QuestionJpaEntity> results = em.createQuery(dataSql, QuestionJpaEntity.class)
                .setFirstResult((page.page() - 1) * page.size())
                .setMaxResults(page.size())
                .getResultList();

        return QuestionReadDtoMapper.toDtoPage(results, total, page);
    }

    @Override
    public PageResult<QuestionDto> findAdminBankQuestions(UUID bankId, Boolean includeArchived, String scope, String status, String type, String keyword, PageRequest page) {
        StringBuilder where = new StringBuilder("WHERE qb.id = :bankId");
        if (!Boolean.TRUE.equals(includeArchived)) {
            where.append(" AND qt.status <> 'ARCHIVED'");
        }
        appendQuestionFilters(where, scope, status, type, keyword);

        String joinClause = "FROM QuestionJpaEntity q JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id ";
        String countSql = "SELECT COUNT(q) " + joinClause + where;
        String dataSql = "SELECT q " + joinClause + where + " ORDER BY q.updatedAt DESC";

        TypedQuery<Long> countQuery = em.createQuery(countSql, Long.class)
                .setParameter("bankId", bankId);
        TypedQuery<QuestionJpaEntity> dataQuery = em.createQuery(dataSql, QuestionJpaEntity.class)
                .setParameter("bankId", bankId);

        bindOptionalQuestionFilters(countQuery, dataQuery, scope, status, type, keyword);

        Long total = countQuery.getSingleResult();
        List<QuestionJpaEntity> results = dataQuery
                .setFirstResult((page.page() - 1) * page.size())
                .setMaxResults(page.size())
                .getResultList();

        return QuestionReadDtoMapper.toDtoPage(results, total, page);
    }

    private void appendQuestionFilters(StringBuilder where, String scope, String status, String type, String keyword) {
        if (scope != null && !scope.isBlank()) {
            where.append(" AND q.scope = :scope");
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND q.status = :status");
        }
        if (type != null && !type.isBlank()) {
            where.append(" AND q.type = :type");
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(q.questionText) LIKE :keyword OR LOWER(q.code) LIKE :keyword)");
        }
    }

    private void bindOptionalQuestionFilters(TypedQuery<Long> countQuery, TypedQuery<QuestionJpaEntity> dataQuery, String scope, String status, String type, String keyword) {
        if (scope != null && !scope.isBlank()) {
            countQuery.setParameter("scope", scope);
            dataQuery.setParameter("scope", scope);
        }
        if (status != null && !status.isBlank()) {
            countQuery.setParameter("status", status);
            dataQuery.setParameter("status", status);
        }
        if (type != null && !type.isBlank()) {
            countQuery.setParameter("type", type);
            dataQuery.setParameter("type", type);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.toLowerCase() + "%";
            countQuery.setParameter("keyword", kw);
            dataQuery.setParameter("keyword", kw);
        }
    }

    private PageResult<QuestionDto> findQuestionsWithJoin(String whereClause, UUID userId, UUID schoolId, PageRequest page) {
        return findQuestionsWithJoinAndFilters(whereClause, userId, schoolId, null, null, null, null, page);
    }

    private PageResult<QuestionDto> findQuestionsWithJoinAndFilters(String whereClause, UUID userId, UUID schoolId, String scope, String status, String type, String keyword, PageRequest page) {
        String joinClause = "FROM QuestionJpaEntity q JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id ";
        String countSql = "SELECT COUNT(q) " + joinClause + whereClause;
        String dataSql = "SELECT q " + joinClause + whereClause + " ORDER BY q.updatedAt DESC";

        TypedQuery<Long> countQuery = em.createQuery(countSql, Long.class);
        TypedQuery<QuestionJpaEntity> dataQuery = em.createQuery(dataSql, QuestionJpaEntity.class);

        if (userId != null) {
            countQuery.setParameter("userId", userId);
            dataQuery.setParameter("userId", userId);
        }
        if (schoolId != null) {
            countQuery.setParameter("schoolId", schoolId);
            dataQuery.setParameter("schoolId", schoolId);
        }

        bindOptionalQuestionFilters(countQuery, dataQuery, scope, status, type, keyword);

        Long total = countQuery.getSingleResult();
        List<QuestionJpaEntity> results = dataQuery
                .setFirstResult((page.page() - 1) * page.size())
                .setMaxResults(page.size())
                .getResultList();

        return QuestionReadDtoMapper.toDtoPage(results, total, page);
    }
}
