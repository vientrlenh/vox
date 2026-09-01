package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;

public interface SpringDataExamRepository extends JpaRepository<ExamJpaEntity, UUID> {

    @Query("""
        SELECT e
        FROM ExamJpaEntity e
        WHERE (:schoolId IS NULL OR e.schoolId = :schoolId)
          AND (
                :schoolClassId IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM ExamCandidateJpaEntity ec
                    JOIN SchoolClassUserJpaEntity scu ON scu.userId = ec.studentId
                    WHERE ec.examId = e.id
                      AND scu.schoolClassId = :schoolClassId
                      AND scu.isActive = true
                )
              )
          AND (:kind IS NULL OR e.kind = :kind)
          AND (:status IS NULL OR e.status = :status)
          AND (
                :keywordPattern IS NULL
                OR LOWER(e.code) LIKE :keywordPattern
                OR LOWER(e.name) LIKE :keywordPattern
              )
          AND (
                :systemAdmin = true
                OR (:schoolAdmin = true AND e.schoolId = :currentSchoolId)
                OR EXISTS (
                    SELECT 1
                    FROM ExamMemberJpaEntity em
                    WHERE em.examId = e.id
                      AND em.userId = :currentUserId
                )
                OR (e.status IN ('CLOSED', 'RESULTS_PUBLISHED') AND e.schoolId = :currentSchoolId)
              )
        ORDER BY e.updatedAt DESC
    """)
    Page<ExamJpaEntity> findAccessible(
        @Param("currentUserId") UUID currentUserId,
        @Param("currentSchoolId") UUID currentSchoolId,
        @Param("systemAdmin") boolean systemAdmin,
        @Param("schoolAdmin") boolean schoolAdmin,
        @Param("schoolId") UUID schoolId,
        @Param("schoolClassId") UUID schoolClassId,
        @Param("kind") String kind,
        @Param("status") String status,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );

    List<com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity> findAllByBlueprintId(UUID blueprintId);
    boolean existsByBlueprintId(UUID blueprintId);

    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
        FROM ExamJpaEntity e
        WHERE e.blueprintId = :blueprintId
          AND e.kind = :kind
          AND e.status <> :status
    """)
    boolean existsByBlueprintIdAndKindAndStatusNot(
        @Param("blueprintId") UUID blueprintId,
        @Param("kind") String kind,
        @Param("status") String status
    );

    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM ExamSessionJpaEntity s
        WHERE s.examId = :examId
          AND s.submittedAt IS NOT NULL
    """)
    boolean existsSubmittedSessionByExamId(@Param("examId") UUID examId);

    /**
     * Bài thi còn chấm được, đang có bài chờ soát điểm AI, và CHƯA nhắc lần nào.
     *
     * <p>Gồm cả IN_PROGRESS lẫn CLOSED có chủ ý: ca thi kết thúc sớm đã ra kết quả ngay khi bài
     * thi còn đang diễn ra, nên chờ tới lúc đóng bài là vứt đi phần lớn thời gian chấm được.
     * CLOSED vẫn nằm trong danh sách để bài đóng trước khi lượt quét kịp chạy không bị bỏ sót.
     *
     * <p>Native + {@code FOR UPDATE SKIP LOCKED} vì cùng lý do với
     * {@code findDueForReminder}: {@code human_grading_notified_at} một mình chỉ chống trùng qua
     * các LƯỢT chạy, không chống trùng giữa các INSTANCE -- hai replica cùng đọc trước khi bên nào
     * commit thì cả hai đều thấy NULL và cùng phát event.
     */
    @Query(value = """
        SELECT * FROM exams e
        WHERE e.status IN ('IN_PROGRESS', 'CLOSED')
        AND e.human_grading_notified_at IS NULL
        AND EXISTS (
            SELECT 1 FROM exam_candidate_results r
            WHERE r.exam_id = e.id AND r.status = 'PENDING_REVIEW'
        )
        ORDER BY e.created_at ASC
        LIMIT 200
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<ExamJpaEntity> findDueForHumanGradingNotice();

    List<ExamJpaEntity> findByStatusAndOpenAtBefore(String status, Instant time);
    List<ExamJpaEntity> findByStatusAndCloseAtBefore(String status, Instant time);
}
