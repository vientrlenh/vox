package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamBlueprintJpaEntity;

public interface SpringDataExamBlueprintRepository extends JpaRepository<ExamBlueprintJpaEntity, UUID> {

    @Query("""
        SELECT b
        FROM ExamBlueprintJpaEntity b
        WHERE (:schoolId IS NULL OR b.schoolId = :schoolId)
          AND (:isActive IS NULL OR b.isActive = :isActive)
          AND (:languageId IS NULL OR b.languageId = :languageId)
          AND (
                :examKind IS NULL
                OR EXISTS (
                    SELECT 1 FROM ExamJpaEntity e
                    WHERE e.blueprintId = b.id AND e.kind = :examKind
                )
              )
          AND (
                :keywordPattern IS NULL
                OR LOWER(b.code) LIKE :keywordPattern
                OR LOWER(b.name) LIKE :keywordPattern
              )
          AND (
                :systemAdmin = true
                OR b.schoolId = :currentSchoolId
              )
        ORDER BY b.updatedAt DESC
    """)
    Page<ExamBlueprintJpaEntity> findAccessible(
        @Param("currentUserId") UUID currentUserId,
        @Param("currentSchoolId") UUID currentSchoolId,
        @Param("systemAdmin") boolean systemAdmin,
        @Param("schoolAdmin") boolean schoolAdmin,
        @Param("schoolId") UUID schoolId,
        @Param("isActive") Boolean isActive,
        @Param("languageId") UUID languageId,
        @Param("examKind") String examKind,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );

    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
        FROM ExamJpaEntity e
        WHERE e.blueprintId = :blueprintId
    """)
    boolean existsUsedByExam(@Param("blueprintId") UUID blueprintId);

    // Nhánh EXISTS thứ hai nhận CHAIR ngang AUTHOR, KHÔNG phải chỉ AUTHOR.
    //
    // Chủ tịch hội đồng là người quyết định của kỳ thi (duyệt/khoá đề, lên lịch, công bố kết
    // quả) mà lại không tự dựng được phiên bản blueprint cho chính kỳ thi mình chủ trì -- phải
    // nhờ một AUTHOR hoặc quản trị trường làm hộ. Quyền hẹp hơn người mình có quyền duyệt là
    // ngược đời.
    //
    // Chú thích phải nằm NGOÀI chuỗi @Query: HQL không có comment `--`, để trong thì
    // SpringDataExamBlueprintRepository không tạo được bean và cả ứng dụng không khởi động nổi
    // (đã xảy ra thật, mọi pod crashloop).
    @Query("""
        SELECT CASE WHEN (
            b.schoolId = :schoolId
            AND (
                EXISTS (
                    SELECT 1 FROM UserRoleJpaEntity ur
                    JOIN RoleJpaEntity r ON r.id = ur.roleId
                    WHERE ur.userId = :userId AND r.code = 'SCHOOL_ADMIN'
                )
                OR EXISTS (
                    SELECT 1 FROM ExamJpaEntity e
                    JOIN ExamMemberJpaEntity em ON em.examId = e.id
                    WHERE e.blueprintId = b.id AND em.userId = :userId
                      AND em.role IN ('AUTHOR', 'CHAIR')
                )
                OR (
                    NOT EXISTS (SELECT 1 FROM ExamJpaEntity e2 WHERE e2.blueprintId = b.id)
                    AND b.createdBy = :userId
                )
            )
        ) THEN true ELSE false END
        FROM ExamBlueprintJpaEntity b
        WHERE b.id = :blueprintId
    """)
    boolean canEditBlueprint(
        @Param("blueprintId") UUID blueprintId,
        @Param("userId") UUID userId,
        @Param("schoolId") UUID schoolId
    );

    @Query("""
        SELECT CASE WHEN (
            b.schoolId = :schoolId
            AND (
                EXISTS (
                    SELECT 1 FROM UserRoleJpaEntity ur
                    JOIN RoleJpaEntity r ON r.id = ur.roleId
                    WHERE ur.userId = :userId AND r.code IN ('SCHOOL_ADMIN', 'SYSTEM_ADMIN')
                )
                OR EXISTS (
                    SELECT 1 FROM ExamJpaEntity e
                    JOIN ExamMemberJpaEntity em ON em.examId = e.id
                    WHERE e.blueprintId = b.id AND em.userId = :userId AND em.role IN ('CHAIR', 'REVIEWER')
                )
            )
        ) THEN true ELSE false END
        FROM ExamBlueprintJpaEntity b
        WHERE b.id = :blueprintId
    """)
    boolean canChangeVersionStatus(
        @Param("blueprintId") UUID blueprintId,
        @Param("userId") UUID userId,
        @Param("schoolId") UUID schoolId
    );

    /**
     * Người này có phải CHAIR của một kỳ thi đang gắn blueprint này không.
     *
     * <p>Hẹp hơn {@link #canChangeVersionStatus} một cách CÓ CHỦ Ý: câu kia gộp
     * {@code IN ('CHAIR', 'REVIEWER')} để trả lời "được đụng vào trạng thái version hay không",
     * còn câu này chỉ dùng cho một việc duy nhất -- quyết định ai được tự duyệt version của
     * chính mình. CHAIR là người chịu trách nhiệm cuối cùng cho kỳ thi nên được; REVIEWER thì
     * vẫn phải có người thứ hai, đó chính là vai trò của họ.
     */
    @Query("""
        SELECT CASE WHEN (
            b.schoolId = :schoolId
            AND EXISTS (
                SELECT 1 FROM ExamJpaEntity e
                JOIN ExamMemberJpaEntity em ON em.examId = e.id
                WHERE e.blueprintId = b.id AND em.userId = :userId AND em.role = 'CHAIR'
            )
        ) THEN true ELSE false END
        FROM ExamBlueprintJpaEntity b
        WHERE b.id = :blueprintId
    """)
    boolean isChairOfExamUsingBlueprint(
        @Param("blueprintId") UUID blueprintId,
        @Param("userId") UUID userId,
        @Param("schoolId") UUID schoolId
    );

    @Query("""
        SELECT CASE WHEN (
            b.schoolId = :schoolId
        ) THEN true ELSE false END
        FROM ExamBlueprintJpaEntity b
        WHERE b.id = :blueprintId
    """)
    boolean canViewBlueprint(
        @Param("blueprintId") UUID blueprintId,
        @Param("userId") UUID userId,
        @Param("schoolId") UUID schoolId
    );
}
