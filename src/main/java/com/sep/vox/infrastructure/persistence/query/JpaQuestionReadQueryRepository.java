package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDetailDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.infrastructure.persistence.entity.QuestionAssetJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.QuestionEvaluationGuideJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.QuestionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.QuestionTopicJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class JpaQuestionReadQueryRepository implements QuestionReadQueryRepository {

    @PersistenceContext
    private EntityManager em;

    // ==================== COMMON ====================

    @Override
    public Optional<QuestionDetailDto> findVisibleQuestionDetail(UUID questionId, UUID userId, String role, UUID schoolId) {
        // Load question
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
                            (q.visibility = 'BANK_VISIBLE' AND q.status = 'PUBLISHED')
                            OR (q.visibility = 'BANK_VISIBLE' AND q.status IN ('DRAFT','SUBMITTED_FOR_REVIEW','REVISION_REQUESTED','APPROVED','REJECTED') AND q.createdBy = :userId)
                            OR (q.visibility = 'AUTHOR_ONLY' AND q.createdBy = :userId)
                            OR (q.visibility = 'REVIEWER_ONLY' AND q.createdBy <> :userId AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId AND q.status = 'SUBMITTED_FOR_REVIEW')
                        ))
                        OR (qb.status = 'DRAFT' AND qt.status <> 'ARCHIVED' AND (
                            q.createdBy = :userId
                            OR (q.visibility = 'REVIEWER_ONLY' AND q.createdBy <> :userId AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId AND q.status = 'SUBMITTED_FOR_REVIEW')
                        ))
                    ))
                    OR (:role = 'SCHOOL_ADMIN' AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId AND qt.status <> 'ARCHIVED' AND q.status <> 'ARCHIVED')
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

        // Load topic
        QuestionTopicJpaEntity topic = em.find(QuestionTopicJpaEntity.class, question.getQuestionTopicId());

        // Load assets
        List<QuestionAssetJpaEntity> assets = em.createQuery(
                "SELECT a FROM QuestionAssetJpaEntity a WHERE a.questionId = :questionId ORDER BY a.order",
                QuestionAssetJpaEntity.class)
                .setParameter("questionId", questionId)
                .getResultList();

        // Load evaluation guide
        QuestionEvaluationGuideJpaEntity guide = null;
        try {
            guide = em.createQuery(
                    "SELECT g FROM QuestionEvaluationGuideJpaEntity g WHERE g.questionId = :questionId",
                    QuestionEvaluationGuideJpaEntity.class)
                    .setParameter("questionId", questionId)
                    .getSingleResult();
        } catch (NoResultException ignored) {
        }

        // Map to DTO
        QuestionTopicDto topicDto = topic != null ? toTopicDto(topic) : null;
        var guideDto = guide != null ? toGuideDto(guide) : null;
        var assetDtos = assets.stream().map(this::toAssetDto).toList();

        return Optional.of(toDetailDto(question, topicDto, guideDto, assetDtos));
    }

    // ==================== TEACHER ====================

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

        return toDtoPage(results, total, page);
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

    // ==================== SCHOOL ====================

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

    // ==================== ADMIN ====================

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

        return toDtoPage(results, total, page);
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

        return toDtoPage(results, total, page);
    }

    // ==================== TEACHER - TOPIC CONTROLLER ====================

    @Override
    public PageResult<QuestionTopicDto> findTeacherBankTopics(UUID bankId, UUID userId, UUID schoolId, PageRequest page) {
        String where = """
            WHERE qt.questionBankId = :bankId
              AND qb.status <> 'ARCHIVED'
              AND qt.status <> 'ARCHIVED'
              AND (
                (qb.status = 'PUBLISHED' AND qt.status = 'PUBLISHED')
                OR (qb.status = 'DRAFT' AND (qb.createdBy = :userId OR (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)))
              )
            """;

        String countSql = "SELECT COUNT(qt) FROM QuestionTopicJpaEntity qt JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id " + where;
        String dataSql = "SELECT qt FROM QuestionTopicJpaEntity qt JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id " + where + " ORDER BY qt.name";

        Long total = em.createQuery(countSql, Long.class)
                .setParameter("bankId", bankId)
                .setParameter("userId", userId)
                .setParameter("schoolId", schoolId)
                .getSingleResult();

        List<QuestionTopicJpaEntity> results = em.createQuery(dataSql, QuestionTopicJpaEntity.class)
                .setParameter("bankId", bankId)
                .setParameter("userId", userId)
                .setParameter("schoolId", schoolId)
                .setFirstResult((page.page() - 1) * page.size())
                .setMaxResults(page.size())
                .getResultList();

        return toTopicDtoPage(results, total, page);
    }

    @Override
    public PageResult<QuestionDto> findTeacherTopicQuestions(UUID bankId, UUID topicId, UUID userId, UUID schoolId, String status, String keyword, PageRequest page) {
        StringBuilder where = new StringBuilder("""
            WHERE qb.id = :bankId AND qt.id = :topicId
              AND qb.status = 'PUBLISHED' AND qt.status = 'PUBLISHED'
              AND (
                (q.visibility = 'BANK_VISIBLE' AND q.status = 'PUBLISHED')
                OR (q.visibility = 'BANK_VISIBLE' AND q.status IN ('DRAFT','SUBMITTED_FOR_REVIEW','REVISION_REQUESTED','APPROVED','REJECTED') AND q.createdBy = :userId)
                OR (q.visibility = 'AUTHOR_ONLY' AND q.createdBy = :userId)
                OR (q.visibility = 'REVIEWER_ONLY' AND q.createdBy <> :userId AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId AND q.status = 'SUBMITTED_FOR_REVIEW')
              )
            """);

        if (status != null && !status.isBlank()) {
            where.append(" AND q.status = :status");
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(q.questionText) LIKE :keyword OR LOWER(q.code) LIKE :keyword)");
        }

        String joinClause = "FROM QuestionJpaEntity q JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id ";
        String countSql = "SELECT COUNT(q) " + joinClause + where;
        String dataSql = "SELECT q " + joinClause + where + " ORDER BY q.updatedAt DESC";

        TypedQuery<Long> countQuery = em.createQuery(countSql, Long.class)
                .setParameter("bankId", bankId)
                .setParameter("topicId", topicId)
                .setParameter("userId", userId)
                .setParameter("schoolId", schoolId);
        TypedQuery<QuestionJpaEntity> dataQuery = em.createQuery(dataSql, QuestionJpaEntity.class)
                .setParameter("bankId", bankId)
                .setParameter("topicId", topicId)
                .setParameter("userId", userId)
                .setParameter("schoolId", schoolId);

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

        return toDtoPage(results, total, page);
    }

    // ==================== SCHOOL - TOPIC CONTROLLER ====================

    @Override
    public PageResult<QuestionTopicDto> findSchoolBankTopics(UUID bankId, UUID schoolId, PageRequest page) {
        String where = """
            WHERE qt.questionBankId = :bankId
              AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
              AND qt.status <> 'ARCHIVED'
            """;

        String countSql = "SELECT COUNT(qt) FROM QuestionTopicJpaEntity qt JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id " + where;
        String dataSql = "SELECT qt FROM QuestionTopicJpaEntity qt JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id " + where + " ORDER BY qt.name";

        Long total = em.createQuery(countSql, Long.class)
                .setParameter("bankId", bankId)
                .setParameter("schoolId", schoolId)
                .getSingleResult();

        List<QuestionTopicJpaEntity> results = em.createQuery(dataSql, QuestionTopicJpaEntity.class)
                .setParameter("bankId", bankId)
                .setParameter("schoolId", schoolId)
                .setFirstResult((page.page() - 1) * page.size())
                .setMaxResults(page.size())
                .getResultList();

        return toTopicDtoPage(results, total, page);
    }

    @Override
    public PageResult<QuestionDto> findSchoolTopicQuestions(UUID bankId, UUID topicId, UUID schoolId, String status, String keyword, PageRequest page) {
        StringBuilder where = new StringBuilder("""
            WHERE qb.id = :bankId AND qt.id = :topicId
              AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
              AND qt.status <> 'ARCHIVED' AND q.status <> 'ARCHIVED'
            """);

        if (status != null && !status.isBlank()) {
            where.append(" AND q.status = :status");
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(q.questionText) LIKE :keyword OR LOWER(q.code) LIKE :keyword)");
        }

        String joinClause = "FROM QuestionJpaEntity q JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id ";
        String countSql = "SELECT COUNT(q) " + joinClause + where;
        String dataSql = "SELECT q " + joinClause + where + " ORDER BY q.updatedAt DESC";

        TypedQuery<Long> countQuery = em.createQuery(countSql, Long.class)
                .setParameter("bankId", bankId)
                .setParameter("topicId", topicId)
                .setParameter("schoolId", schoolId);
        TypedQuery<QuestionJpaEntity> dataQuery = em.createQuery(dataSql, QuestionJpaEntity.class)
                .setParameter("bankId", bankId)
                .setParameter("topicId", topicId)
                .setParameter("schoolId", schoolId);

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

        return toDtoPage(results, total, page);
    }

    // ==================== ADMIN - TOPIC CONTROLLER ====================

    @Override
    public PageResult<QuestionTopicDto> findAdminBankTopics(UUID bankId, Boolean includeArchived, PageRequest page) {
        StringBuilder where = new StringBuilder("WHERE qt.questionBankId = :bankId");
        if (!Boolean.TRUE.equals(includeArchived)) {
            where.append(" AND qt.status <> 'ARCHIVED'");
        }

        String countSql = "SELECT COUNT(qt) FROM QuestionTopicJpaEntity qt " + where;
        String dataSql = "SELECT qt FROM QuestionTopicJpaEntity qt " + where + " ORDER BY qt.name";

        Long total = em.createQuery(countSql, Long.class)
                .setParameter("bankId", bankId)
                .getSingleResult();

        List<QuestionTopicJpaEntity> results = em.createQuery(dataSql, QuestionTopicJpaEntity.class)
                .setParameter("bankId", bankId)
                .setFirstResult((page.page() - 1) * page.size())
                .setMaxResults(page.size())
                .getResultList();

        return toTopicDtoPage(results, total, page);
    }

    // ==================== ADMIN - BANK CONTROLLER ====================

    @Override
    public PageResult<QuestionDto> findAdminBankQuestions(UUID bankId, Boolean includeArchived, String status, String keyword, PageRequest page) {
        StringBuilder where = new StringBuilder("WHERE qb.id = :bankId");
        if (!Boolean.TRUE.equals(includeArchived)) {
            where.append(" AND qt.status <> 'ARCHIVED'");
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND q.status = :status");
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(q.questionText) LIKE :keyword OR LOWER(q.code) LIKE :keyword)");
        }

        String joinClause = "FROM QuestionJpaEntity q JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id ";
        String countSql = "SELECT COUNT(q) " + joinClause + where;
        String dataSql = "SELECT q " + joinClause + where + " ORDER BY q.updatedAt DESC";

        TypedQuery<Long> countQuery = em.createQuery(countSql, Long.class)
                .setParameter("bankId", bankId);
        TypedQuery<QuestionJpaEntity> dataQuery = em.createQuery(dataSql, QuestionJpaEntity.class)
                .setParameter("bankId", bankId);

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

        return toDtoPage(results, total, page);
    }

    // ==================== HELPER METHODS ====================

    private PageResult<QuestionDto> findQuestionsWithJoin(String whereClause, UUID userId, UUID schoolId, PageRequest page) {
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

        Long total = countQuery.getSingleResult();
        List<QuestionJpaEntity> results = dataQuery
                .setFirstResult((page.page() - 1) * page.size())
                .setMaxResults(page.size())
                .getResultList();

        return toDtoPage(results, total, page);
    }

    private PageResult<QuestionDto> toDtoPage(List<QuestionJpaEntity> entities, Long total, PageRequest page) {
        List<QuestionDto> dtos = entities.stream().map(this::toDto).toList();
        int totalPages = (int) Math.ceil((double) total / page.size());
        return new PageResult<>(dtos, page.page(), page.size(), total, totalPages);
    }

    private PageResult<QuestionTopicDto> toTopicDtoPage(List<QuestionTopicJpaEntity> entities, Long total, PageRequest page) {
        List<QuestionTopicDto> dtos = entities.stream().map(this::toTopicDto).toList();
        int totalPages = (int) Math.ceil((double) total / page.size());
        return new PageResult<>(dtos, page.page(), page.size(), total, totalPages);
    }

    private QuestionDto toDto(QuestionJpaEntity e) {
        return new QuestionDto(
                e.getId(),
                e.getQuestionTopicId(),
                e.getCode(),
                e.getInstructionText(),
                e.getQuestionText(),
                e.getPromptText(),
                e.getPreparationText(),
                e.getType(),
                e.getPreparationTimeSeconds(),
                e.getMinResponseSeconds(),
                e.getMaxResponseSeconds(),
                e.getScope(),
                e.getVisibility(),
                e.getSourceQuestionId(),
                e.isLocked(),
                e.getStatus(),
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null
        );
    }

    private QuestionTopicDto toTopicDto(QuestionTopicJpaEntity e) {
        return new QuestionTopicDto(
                e.getId(),
                e.getQuestionBankId(),
                e.getCode(),
                e.getName(),
                e.getDescription(),
                e.getStatus(),
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null
        );
    }

    private QuestionDetailDto toDetailDto(QuestionJpaEntity q, QuestionTopicDto topic,
            com.sep.vox.domain.dto.QuestionEvaluationGuideDto guide,
            List<com.sep.vox.domain.dto.QuestionAssetDto> assets) {
        return new QuestionDetailDto(
                q.getId(),
                q.getQuestionTopicId(),
                q.getCode(),
                q.getInstructionText(),
                q.getQuestionText(),
                q.getPromptText(),
                q.getPreparationText(),
                q.getType(),
                q.getPreparationTimeSeconds(),
                q.getMinResponseSeconds(),
                q.getMaxResponseSeconds(),
                q.getScope(),
                q.getVisibility(),
                q.getSourceQuestionId(),
                q.isLocked(),
                q.getStatus(),
                q.getCreatedAt() != null ? q.getCreatedAt().toString() : null,
                q.getUpdatedAt() != null ? q.getUpdatedAt().toString() : null,
                topic,
                guide,
                assets
        );
    }

    private com.sep.vox.domain.dto.QuestionEvaluationGuideDto toGuideDto(QuestionEvaluationGuideJpaEntity e) {
        return new com.sep.vox.domain.dto.QuestionEvaluationGuideDto(
                e.getId(),
                e.getQuestionId(),
                e.getExpectedContent(),
                e.getKeyPoints(),
                e.getAcceptableResponses(),
                e.getOffTopicExamples(),
                e.getScoringHints(),
                e.getCommonMistakes()
        );
    }

    private com.sep.vox.domain.dto.QuestionAssetDto toAssetDto(QuestionAssetJpaEntity e) {
        return new com.sep.vox.domain.dto.QuestionAssetDto(
                e.getId(),
                e.getQuestionId(),
                e.getTitle(),
                e.getDurationSeconds(),
                e.getAltText(),
                e.getType(),
                e.getUrl(),
                e.getTranscript(),
                e.getDescription(),
                e.getOrder()
        );
    }
}
