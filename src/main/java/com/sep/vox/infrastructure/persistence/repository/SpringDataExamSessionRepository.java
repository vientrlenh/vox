package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.ExamSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataExamSessionRepository extends JpaRepository<ExamSessionJpaEntity, UUID> {
    Optional<ExamSessionJpaEntity> findTopByExamIdAndCandidateIdOrderByStartedAtDesc(UUID examId, UUID candidateId);
    Optional<ExamSessionJpaEntity> findTopByCandidateIdOrderByStartedAtDesc(UUID candidateId);
    Optional<ExamSessionJpaEntity> findTopByCandidateIdAndStatusInOrderByStartedAtDesc(UUID candidateId, Collection<String> statuses);
    List<ExamSessionJpaEntity> findByCandidateId(UUID candidateId);
    List<ExamSessionJpaEntity> findByCandidateIdIn(Collection<UUID> candidateIds);

    @Query("""
        SELECT s
        FROM ExamSessionJpaEntity s
        JOIN ExamJpaEntity e ON e.id = s.examId
        JOIN ExamCandidateJpaEntity c ON c.id = s.candidateId
        WHERE (e.status IN ('CLOSED', 'CANCELLED') OR (e.closeAt IS NOT NULL AND e.closeAt < :now))
          AND (
                s.status = 'EXPIRED'
                OR s.status = 'SUBMITTED'
                OR s.status IN ('IN_PROGRESS', 'INTERRUPTED')
              )
          AND (
                (e.kind = 'CENTRALIZED' AND c.status = 'ATTENDED')
                OR (e.kind = 'CLASS_TEST' AND c.status NOT IN ('ABSENT', 'EXEMPTED', 'CANCELLED'))
              )
          AND s.status NOT IN ('GRADING', 'GRADED', 'GRADING_FAILED')
        ORDER BY s.startedAt ASC
    """)
    List<ExamSessionJpaEntity> findDeferredGradingCandidates(@Param("now") java.time.OffsetDateTime now);

    @Query("""
        SELECT s
        FROM ExamSessionJpaEntity s
        JOIN ExamCandidateJpaEntity c ON c.id = s.candidateId
        JOIN ExamScheduleJpaEntity sch ON sch.id = c.scheduleId
        WHERE sch.endDate < :threshold
          AND s.status IN ('IN_PROGRESS', 'INTERRUPTED')
          AND c.status = 'ATTENDED'
        ORDER BY sch.endDate ASC, s.startedAt ASC
    """)
    List<ExamSessionJpaEntity> findPastScheduleEndCandidates(@Param("threshold") java.time.OffsetDateTime threshold);
    Optional<ExamSessionJpaEntity> findByExamIdAndCandidateIdAndStatus(UUID examId, UUID candidateId, String status);
    Optional<ExamSessionJpaEntity> findByIdAndStatusIn(UUID id, Collection<String> statuses);

    @Query("""
        SELECT s 
        FROM ExamSessionJpaEntity s 
        JOIN ExamJpaEntity e 
            ON s.examId = e.id 
        WHERE s.id IN :ids
            AND s.startedAt <= :now 
            AND s.submittedAt IS NULL 
            AND s.status = 'IN_PROGRESS' 
            AND e.schoolId = :schoolId
    """)
    List<ExamSessionJpaEntity> findActiveByIdInAndSchoolId(@Param("ids") Collection<UUID> ids, @Param("now") OffsetDateTime now, @Param("schoolId") UUID schoolId);

    /**
     * Chốt loại stream cho phiên thi, chỉ khi chưa từng chốt (compare-and-set).
     *
     * <p>Điều kiện {@code chosenStreamType IS NULL} chính là phần khóa: hai request phát token
     * chạy song song (bootstrap lúc vào thi và lần gia hạn credential đầu tiên) sẽ chỉ có một cái
     * ghi được, cái còn lại nhận 0 dòng và phải đọc lại giá trị đã thắng. Nếu dùng
     * {@code save(entity)} thay cho câu lệnh này thì vừa mất tính nguyên tử, vừa ghi đè cả những
     * cột khác của phiên thi (status, flagged...) bằng bản snapshot cũ đã đọc trước đó.
     *
     * @return số dòng được cập nhật: 1 nếu chốt thành công, 0 nếu phiên thi đã chốt từ trước.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ExamSessionJpaEntity s
        SET s.chosenStreamType = :chosenStreamType
        WHERE s.id = :id AND s.chosenStreamType IS NULL
    """)
    int lockChosenStreamType(@Param("id") UUID id, @Param("chosenStreamType") String chosenStreamType);
}
