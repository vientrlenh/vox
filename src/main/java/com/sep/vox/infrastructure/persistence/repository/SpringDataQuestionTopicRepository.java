package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.QuestionTopicJpaEntity;

public interface SpringDataQuestionTopicRepository extends JpaRepository<QuestionTopicJpaEntity, UUID> {
    List<QuestionTopicJpaEntity> findByQuestionBankId(UUID bankId);
    Page<QuestionTopicJpaEntity> findByQuestionBankId(UUID bankId, Pageable pageable);

    @Query("""
        SELECT CASE WHEN COUNT(qt) > 0 THEN true ELSE false END
        FROM QuestionTopicJpaEntity qt 
        JOIN QuestionBankJpaEntity qb 
            ON qt.questionBankId = qb.id 
        JOIN SchoolJpaEntity s 
            ON qb.schoolId = s.id
        WHERE qt.id = :questionTopicId 
            AND s.id = :schoolId
    """)
    boolean isTopicBelongToSchool(@Param("questionTopicId") UUID questionTopicId, @Param("schoolId") UUID schoolId);

    @Query("""
        SELECT qt
        FROM QuestionTopicJpaEntity qt
        JOIN QuestionBankJpaEntity qb ON qb.id = qt.questionBankId
        WHERE (:questionBankId IS NULL OR qt.questionBankId = :questionBankId)
          AND (:status IS NULL OR qt.status = :status)
          AND (
                :keywordPattern IS NULL
                OR LOWER(qt.code) LIKE :keywordPattern
                OR LOWER(qt.name) LIKE :keywordPattern
              )
          AND (
                :systemAdmin = true
                OR (:schoolAdmin = true AND qb.schoolId = :currentSchoolId)
                OR (qb.ownerType = 'SYSTEM' AND qb.status = 'PUBLISHED' AND qt.status = 'PUBLISHED')
                OR (:schoolAdmin = false AND qb.schoolId = :currentSchoolId AND qb.status = 'PUBLISHED' AND qt.status = 'PUBLISHED')
              )
        ORDER BY qt.updatedAt DESC
    """)
    Page<QuestionTopicJpaEntity> findAccessible(
        @Param("currentSchoolId") UUID currentSchoolId,
        @Param("systemAdmin") boolean systemAdmin,
        @Param("schoolAdmin") boolean schoolAdmin,
        @Param("questionBankId") UUID questionBankId,
        @Param("status") String status,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );
}
