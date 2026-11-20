package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamSessionJpaEntity;


public interface SpringDataExamSessionRepository extends JpaRepository<ExamSessionJpaEntity, UUID> {

    // Bốn câu dưới đây LOẠI phiên đã xoá mềm: chúng phục vụ luồng nghiệp vụ (vào thi, đếm lượt,
    // chốt điểm, đóng kỳ thi) chứ không phải màn tra cứu của quản trị, nên một phiên đã xoá không
    // được tính là "thí sinh đã có bài". Các câu còn lại trong file đã lọc theo danh sách trạng
    // thái dương (IN_PROGRESS, SUBMITTED...) nên tự khắc không dính DELETED.
    @Query("""
        SELECT s FROM ExamSessionJpaEntity s
        WHERE s.examId = :examId AND s.candidateId = :candidateId AND s.status <> 'DELETED'
        ORDER BY s.startedAt DESC
        LIMIT 1
    """)
    Optional<ExamSessionJpaEntity> findTopByExamIdAndCandidateIdOrderByStartedAtDesc(
        @Param("examId") UUID examId, @Param("candidateId") UUID candidateId);

    @Query("""
        SELECT s FROM ExamSessionJpaEntity s
        WHERE s.candidateId = :candidateId AND s.status <> 'DELETED'
        ORDER BY s.startedAt DESC
        LIMIT 1
    """)
    Optional<ExamSessionJpaEntity> findTopByCandidateIdOrderByStartedAtDesc(@Param("candidateId") UUID candidateId);

    Optional<ExamSessionJpaEntity> findTopByCandidateIdAndStatusInOrderByStartedAtDesc(UUID candidateId, Collection<String> statuses);

    @Query("SELECT s FROM ExamSessionJpaEntity s WHERE s.candidateId = :candidateId AND s.status <> 'DELETED'")
    List<ExamSessionJpaEntity> findByCandidateId(@Param("candidateId") UUID candidateId);

    @Query("SELECT s FROM ExamSessionJpaEntity s WHERE s.candidateId IN :candidateIds AND s.status <> 'DELETED'")
    List<ExamSessionJpaEntity> findByCandidateIdIn(@Param("candidateIds") Collection<UUID> candidateIds);

    /**
     * CỐ Ý tính cả phiên đã xoá mềm: dòng đã xoá vẫn giữ {@code paper_id}, nên nếu cho xoá mã đề đi
     * thì phiên ấy trỏ vào một mã đề không còn tồn tại và quản trị hết đường mở lại để đối chiếu —
     * đúng thứ mà xoá mềm sinh ra để bảo toàn.
     */
    boolean existsByPaperId(UUID paperId);

    /**
     * Đếm phiên còn đang làm bài của một kỳ thi. Bỏ qua thí sinh đã bị đình chỉ giống
     * {@code findPastScheduleEndCandidates} bên dưới -- phiên của họ không tự kết thúc.
     */
    @Query("""
        SELECT COUNT(s)
        FROM ExamSessionJpaEntity s
        JOIN ExamCandidateJpaEntity c ON c.id = s.candidateId
        WHERE s.examId = :examId
          AND s.status IN ('IN_PROGRESS', 'INTERRUPTED')
          AND c.blockedAt IS NULL
    """)
    long countActiveByExamId(@Param("examId") UUID examId);

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
    List<ExamSessionJpaEntity> findDeferredGradingCandidates(@Param("now") Instant now);

    @Query("""
        SELECT s
        FROM ExamSessionJpaEntity s
        JOIN ExamCandidateJpaEntity c ON c.id = s.candidateId
        JOIN ExamScheduleJpaEntity sch ON sch.id = c.scheduleId
        WHERE sch.endDate < :threshold
          AND s.status IN ('IN_PROGRESS', 'INTERRUPTED')
          AND s.status NOT IN ('GRADING', 'GRADED', 'GRADING_FAILED')
          AND c.status = 'ATTENDED'
        ORDER BY sch.endDate ASC, s.startedAt ASC
    """)
    List<ExamSessionJpaEntity> findPastScheduleEndCandidates(@Param("threshold") java.time.Instant threshold);
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
    List<ExamSessionJpaEntity> findActiveByIdInAndSchoolId(@Param("ids") Collection<UUID> ids, @Param("now") Instant now, @Param("schoolId") UUID schoolId);

    @Modifying
    @Query("UPDATE ExamSessionJpaEntity s SET s.status = :to WHERE s.id = :id AND s.status = :from")
    int tryTransitionStatus(@Param("id") UUID id, @Param("from") String from, @Param("to") String to);

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

    /**
     * Ghi checkpoint đồng hồ đếm ngược, chỉ khi giá trị mới nhỏ hơn giá trị đang có.
     *
     * <p>Điều kiện {@code remainingSeconds > :remainingSeconds} không phải để tối ưu mà là ràng
     * buộc bảo mật: giá trị này do máy học viên gửi lên, nên nếu cho phép ghi đè tự do thì endpoint
     * checkpoint trở thành API tự gia hạn thời gian thi. Đồng hồ chỉ được đi lùi.
     *
     * <p>Đồng thời xử lý luôn chuyện đua: client checkpoint 10 giây một lần và các request có thể
     * đến không đúng thứ tự, một gói cũ đến muộn sẽ bị chính điều kiện này loại.
     *
     * @return 1 nếu ghi được, 0 nếu giá trị gửi lên không nhỏ hơn giá trị đã lưu.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ExamSessionJpaEntity s
        SET s.remainingSeconds = :remainingSeconds
        WHERE s.id = :id
          AND (s.remainingSeconds IS NULL OR s.remainingSeconds > :remainingSeconds)
    """)
    int checkpointRemainingSeconds(@Param("id") UUID id, @Param("remainingSeconds") int remainingSeconds);

    /**
     * Xoá mềm phiên thi: đánh dấu {@code DELETED} kèm mốc thời gian và lý do.
     *
     * <p>Điều kiện {@code deletedAt IS NULL} làm thao tác này idempotent — bấm xoá hai lần (hoặc hai
     * người cùng bấm) thì lần sau nhận 0 dòng chứ không ghi đè lý do và thời điểm của lần đầu, vốn
     * là thứ cần giữ nguyên khi phải giải trình.
     *
     * @return 1 nếu vừa xoá, 0 nếu phiên đã bị xoá từ trước.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ExamSessionJpaEntity s
        SET s.status = 'DELETED', s.deletedAt = :deletedAt, s.deletedReason = :reason
        WHERE s.id = :id AND s.deletedAt IS NULL
    """)
    int softDelete(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt, @Param("reason") String reason);
}
