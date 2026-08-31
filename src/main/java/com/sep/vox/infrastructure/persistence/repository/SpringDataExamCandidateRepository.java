package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamCandidateJpaEntity;

public interface SpringDataExamCandidateRepository extends JpaRepository<ExamCandidateJpaEntity, UUID> {
    /**
     * Danh sách thí sinh của kỳ thi, thứ tự CỐ ĐỊNH theo lúc được thêm vào.
     *
     * <p>Không có ORDER BY thì Postgres trả về theo thứ tự vật lý của heap: thứ tự đổi sau mỗi
     * lần ghi và không giống nhau giữa hai lần gọi. Màn danh sách thí sinh phân trang 10 dòng
     * trên chính mảng này, nên thứ tự trôi làm người vừa thêm nhảy trang — thêm xong không thấy
     * đâu, F5 lại thấy. `assignedAt` là `updatable = false`; kèm `id` để nhập theo lớp/khối
     * (cả loạt chung một mốc thời gian) vẫn có thứ tự xác định.
     */
    List<ExamCandidateJpaEntity> findByExamIdOrderByAssignedAtAscIdAsc(UUID examId);
    List<ExamCandidateJpaEntity> findByExamIdIn(Collection<UUID> examIds);
    void deleteByExamId(UUID examId);
    void deleteByIdIn(Collection<UUID> ids);
    Optional<ExamCandidateJpaEntity> findByExamIdAndStudentId(UUID examId, UUID studentId);
    List<ExamCandidateJpaEntity> findByStudentId(UUID studentId);
    List<ExamCandidateJpaEntity> findByScheduleId(UUID scheduleId);
    List<ExamCandidateJpaEntity> findByAssignedPaperId(UUID paperId);
    long countByExamId(UUID examId);
    boolean existsByExamIdAndStudentId(UUID examId, UUID studentId);

    interface ExamIdCandidateCount {
        UUID getExamId();
        long getCandidateCount();
    }

    @Query("""
        SELECT c.examId AS examId, COUNT(c) AS candidateCount
        FROM ExamCandidateJpaEntity c
        WHERE c.examId IN :examIds
        GROUP BY c.examId
    """)
    List<ExamIdCandidateCount> countByExamIdIn(@Param("examIds") Collection<UUID> examIds);

    @Query("SELECT c.studentId FROM ExamCandidateJpaEntity c WHERE c.examId = :examId")
    List<UUID> findStudentIdsByExamId(UUID examId);

    @Query("""
        SELECT DISTINCT c.studentId FROM ExamCandidateJpaEntity c
        WHERE c.examId = :examId AND c.blockedAt IS NULL
        ORDER BY c.studentId
        """)
    List<UUID> findDistinctUnblockedStudentIdsByExamId(@Param("examId") UUID examId);
    List<ExamCandidateJpaEntity> findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(UUID examId);
    List<ExamCandidateJpaEntity> findByIdInAndExamId(Collection<UUID> ids, UUID examId);
    long countByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);
    boolean existsByExamIdAndScheduleIdIsNotNull(UUID examId);
    Optional<ExamCandidateJpaEntity> findByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);

    @Query("""
        SELECT c
            FROM ExamCandidateJpaEntity c
        JOIN ExamScheduleJpaEntity s
            ON c.scheduleId = s.id
        WHERE c.studentId = :userId
            AND s.startDate <= :now
            AND s.endDate > :now
    """)
    List<ExamCandidateJpaEntity> findActiveCandidate(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Với mỗi học sinh trong {@code studentIds}, ca thi (DRAFT/PUBLISHED) mà họ đã được xếp và giao
     * thời gian với [start, end).
     *
     * <p>Giờ nằm ở bảng cha nên phải join sang {@code exam_schedules}. Điều kiện giao khoảng dùng
     * đúng dạng nửa mở như {@code SpringDataExamScheduleProctorRepository.countOverlappingAssignments}:
     * hai ca kề nhau (ca trước kết thúc đúng lúc ca sau bắt đầu) KHÔNG tính là trùng.
     *
     * <p>Thí sinh đã miễn thi hoặc đã huỷ không vào phòng nên không chiếm chỗ -- cùng cách phân loại
     * với {@code ExamCandidateStatus.isNonScorable}. Trạng thái lưu dạng chuỗi nên phải liệt kê
     * literal ở đây; đổi enum thì phải sửa cả hai chỗ.
     */
    @Query("""
        SELECT c.studentId, s.id, s.startDate, s.endDate
        FROM ExamCandidateJpaEntity c
        JOIN ExamScheduleJpaEntity s ON s.id = c.scheduleId
        WHERE c.studentId IN :studentIds
          AND c.status NOT IN ('EXEMPTED', 'CANCELLED')
          AND s.status IN ('DRAFT', 'PUBLISHED')
          AND (:excludeScheduleId IS NULL OR s.id <> :excludeScheduleId)
          AND s.startDate < :end AND s.endDate > :start
        ORDER BY s.startDate
        """)
    List<Object[]> findOverlappingScheduleAssignments(
        @Param("studentIds") Collection<UUID> studentIds,
        @Param("start") Instant start,
        @Param("end") Instant end,
        @Param("excludeScheduleId") UUID excludeScheduleId);
}

