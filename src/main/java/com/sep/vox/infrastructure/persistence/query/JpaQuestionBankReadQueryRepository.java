package com.sep.vox.infrastructure.persistence.query;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.repository.QuestionBankReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.infrastructure.persistence.entity.QuestionBankJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaQuestionBankReadQueryRepository implements QuestionBankReadQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public PageResult<QuestionBankDto> findAdminQuestionBanks(PageRequest pageRequest) {
        var totalElements = em.createQuery("""
            SELECT COUNT(qb) FROM QuestionBankJpaEntity qb
            """, Long.class).getSingleResult();

        var content = em.createQuery("""
            SELECT qb FROM QuestionBankJpaEntity qb
            ORDER BY qb.createdAt DESC
            """, QuestionBankJpaEntity.class)
            .setFirstResult((pageRequest.page() - 1) * pageRequest.size())
            .setMaxResults(pageRequest.size())
            .getResultList()
            .stream()
            .map(this::toDto)
            .toList();

        var totalPages = (int) Math.ceil((double) totalElements / pageRequest.size());
        return new PageResult<>(content, pageRequest.page(), pageRequest.size(), totalElements, totalPages);
    }

    @Override
    public PageResult<QuestionBankDto> findAdminSchoolQuestionBanks(UUID schoolId, PageRequest pageRequest) {
        var totalElements = em.createQuery("""
            SELECT COUNT(qb) FROM QuestionBankJpaEntity qb
            WHERE qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
            """, Long.class)
            .setParameter("schoolId", schoolId)
            .getSingleResult();

        var content = em.createQuery("""
            SELECT qb FROM QuestionBankJpaEntity qb
            WHERE qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
            ORDER BY qb.createdAt DESC
            """, QuestionBankJpaEntity.class)
            .setParameter("schoolId", schoolId)
            .setFirstResult((pageRequest.page() - 1) * pageRequest.size())
            .setMaxResults(pageRequest.size())
            .getResultList()
            .stream()
            .map(this::toDto)
            .toList();

        var totalPages = (int) Math.ceil((double) totalElements / pageRequest.size());
        return new PageResult<>(content, pageRequest.page(), pageRequest.size(), totalElements, totalPages);
    }

    @Override
    public PageResult<QuestionBankDto> findTeacherQuestionBanks(UUID userId, UUID schoolId, PageRequest pageRequest) {
        var countQuery = em.createQuery("""
            SELECT COUNT(qb) FROM QuestionBankJpaEntity qb
            WHERE qb.status = 'PUBLISHED'
            AND (
                qb.ownerType = 'SYSTEM'
                OR (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)
            )
            """, Long.class)
            .setParameter("schoolId", schoolId);
        var totalElements = countQuery.getSingleResult();

        var query = em.createQuery("""
            SELECT qb FROM QuestionBankJpaEntity qb
            WHERE qb.status = 'PUBLISHED'
            AND (
                qb.ownerType = 'SYSTEM'
                OR (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)
            )
            ORDER BY qb.createdAt DESC
            """, QuestionBankJpaEntity.class)
            .setParameter("schoolId", schoolId)
            .setFirstResult((pageRequest.page() - 1) * pageRequest.size())
            .setMaxResults(pageRequest.size());

        var content = query.getResultList().stream().map(this::toDto).toList();
        var totalPages = (int) Math.ceil((double) totalElements / pageRequest.size());
        return new PageResult<>(content, pageRequest.page(), pageRequest.size(), totalElements, totalPages);
    }

    @Override
    public Optional<QuestionBankDto> findTeacherQuestionBank(UUID bankId, UUID userId, UUID schoolId) {
        try {
            var entity = em.createQuery("""
                SELECT qb FROM QuestionBankJpaEntity qb
                WHERE qb.id = :bankId
                AND qb.status = 'PUBLISHED'
                AND (
                    qb.ownerType = 'SYSTEM'
                    OR (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)
                )
                """, QuestionBankJpaEntity.class)
                .setParameter("bankId", bankId)
                .setParameter("schoolId", schoolId)
                .getSingleResult();
            return Optional.of(toDto(entity));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public PageResult<QuestionBankDto> findSchoolQuestionBanks(UUID schoolId, PageRequest pageRequest) {
        var countQuery = em.createQuery("""
            SELECT COUNT(qb) FROM QuestionBankJpaEntity qb
            WHERE (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)
               OR (qb.ownerType = 'SYSTEM' AND qb.status = 'PUBLISHED')
            """, Long.class)
            .setParameter("schoolId", schoolId);
        var totalElements = countQuery.getSingleResult();

        var query = em.createQuery("""
            SELECT qb FROM QuestionBankJpaEntity qb
            WHERE (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)
               OR (qb.ownerType = 'SYSTEM' AND qb.status = 'PUBLISHED')
            ORDER BY qb.createdAt DESC
            """, QuestionBankJpaEntity.class)
            .setParameter("schoolId", schoolId)
            .setFirstResult((pageRequest.page() - 1) * pageRequest.size())
            .setMaxResults(pageRequest.size());

        var content = query.getResultList().stream().map(this::toDto).toList();
        var totalPages = (int) Math.ceil((double) totalElements / pageRequest.size());
        return new PageResult<>(content, pageRequest.page(), pageRequest.size(), totalElements, totalPages);
    }

    @Override
    public Optional<QuestionBankDto> findSchoolQuestionBank(UUID bankId, UUID schoolId) {
        try {
            var entity = em.createQuery("""
                SELECT qb FROM QuestionBankJpaEntity qb
                WHERE qb.id = :bankId
                AND (
                    (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)
                    OR (qb.ownerType = 'SYSTEM' AND qb.status = 'PUBLISHED')
                )
                """, QuestionBankJpaEntity.class)
                .setParameter("bankId", bankId)
                .setParameter("schoolId", schoolId)
                .getSingleResult();
            return Optional.of(toDto(entity));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<QuestionBankDto> findAdminQuestionBank(UUID bankId) {
        try {
            var entity = em.createQuery("""
                SELECT qb FROM QuestionBankJpaEntity qb
                WHERE qb.id = :bankId
                """, QuestionBankJpaEntity.class)
                .setParameter("bankId", bankId)
                .getSingleResult();
            return Optional.of(toDto(entity));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    private QuestionBankDto toDto(QuestionBankJpaEntity entity) {
        return new QuestionBankDto(
            entity.getId(),
            entity.getLanguageId(),
            entity.getCode(),
            entity.getName(),
            entity.getDescription(),
            "PUBLISHED".equals(entity.getStatus()),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null
        );
    }
}
