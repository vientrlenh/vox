package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.QuestionBankJpaEntity;

public interface SpringDataQuestionBankRepository extends JpaRepository<QuestionBankJpaEntity, UUID> {

    /**
     * Trọn danh sách ngân hàng của MỘT phạm vi sở hữu, cho import đối chiếu trùng code.
     * {@code schoolId} null nghĩa là phạm vi hệ thống (ngân hàng SYSTEM không thuộc trường nào),
     * nên phải so bằng {@code IS NULL} chứ không so bằng tham số.
     */
    @Query("""
        SELECT qb
        FROM QuestionBankJpaEntity qb
        WHERE qb.ownerType = :ownerType
          AND ((:schoolId IS NULL AND qb.schoolId IS NULL) OR qb.schoolId = :schoolId)
        """)
    List<QuestionBankJpaEntity> findByOwnerScope(
        @Param("ownerType") String ownerType,
        @Param("schoolId") UUID schoolId
    );

    @Query("""
        SELECT qb
        FROM QuestionBankJpaEntity qb
        WHERE (:ownerType IS NULL OR qb.ownerType = :ownerType)
          AND (:status IS NULL OR qb.status = :status)
          AND (:languageId IS NULL OR qb.languageId = :languageId)
          AND (:schoolId IS NULL OR qb.schoolId = :schoolId)
          AND (
                :schoolGradeId IS NULL
                OR EXISTS (
                    SELECT 1 FROM QuestionBankGradeJpaEntity qbg
                    WHERE qbg.questionBankId = qb.id AND qbg.schoolGradeId = :schoolGradeId
                )
              )
          AND (
                :keywordPattern IS NULL
                OR LOWER(qb.code) LIKE :keywordPattern
                OR LOWER(qb.name) LIKE :keywordPattern
              )
          AND (
                :systemAdmin = true
                OR (:schoolAdmin = true AND qb.schoolId = :currentSchoolId)
                OR (qb.ownerType = 'SYSTEM' AND qb.status = 'PUBLISHED')
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
        @Param("schoolGradeId") UUID schoolGradeId,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );
}
