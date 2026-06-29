package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.QuestionBankJpaEntity;

public interface SpringDataQuestionBankRepository extends JpaRepository<QuestionBankJpaEntity, UUID> {
    @Query("""
        SELECT qb
        FROM QuestionBankJpaEntity qb
        WHERE (:ownerType IS NULL OR qb.ownerType = :ownerType)
          AND (:status IS NULL OR qb.status = :status)
          AND (:languageId IS NULL OR qb.languageId = :languageId)
          AND (:schoolId IS NULL OR qb.schoolId = :schoolId)
          AND (
                :keyword IS NULL
                OR LOWER(qb.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(qb.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
          AND (
                :systemAdmin = true
                OR (:schoolAdmin = true AND qb.schoolId = :currentSchoolId)
                OR (:schoolAdmin = true AND qb.ownerType = 'SYSTEM' AND qb.status = 'PUBLISHED')
                OR (:schoolAdmin = false AND qb.schoolId = :currentSchoolId AND qb.status = 'PUBLISHED')
              )
        ORDER BY qb.updatedAt DESC
        """)
    Page<QuestionBankJpaEntity> findAccessible(
        @Param("currentSchoolId") UUID currentSchoolId,
        @Param("systemAdmin") boolean systemAdmin,
        @Param("schoolAdmin") boolean schoolAdmin,
        @Param("ownerType") String ownerType,
        @Param("status") String status,
        @Param("languageId") UUID languageId,
        @Param("schoolId") UUID schoolId,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
