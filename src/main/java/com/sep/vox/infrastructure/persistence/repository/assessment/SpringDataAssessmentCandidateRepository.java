package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateJpaEntity;

public interface SpringDataAssessmentCandidateRepository extends JpaRepository<AssessmentCandidateJpaEntity, UUID> {
    /**
     * Danh sách thí sinh của kỳ thi, thứ tự CỐ ĐỊNH theo lúc được thêm vào.
     *
     * <p>Không có ORDER BY thì Postgres trả về theo thứ tự vật lý của heap: thứ tự đổi sau mỗi
     * lần ghi và không giống nhau giữa hai lần gọi. Màn danh sách thí sinh phân trang 10 dòng
     * trên chính mảng này, nên thứ tự trôi làm người vừa thêm nhảy trang — thêm xong không thấy
     * đâu, F5 lại thấy. `assignedAt` là `updatable = false`; kèm `id` để nhập theo lớp/khối
     * (cả loạt chung một mốc thời gian) vẫn có thứ tự xác định.
     */
    List<AssessmentCandidateJpaEntity> findByAssessmentIdOrderByAssignedAtAscIdAsc(UUID assessmentId);
    List<AssessmentCandidateJpaEntity> findByAssessmentIdIn(Collection<UUID> assessmentIds);
    void deleteByAssessmentId(UUID assessmentId);
    void deleteByIdIn(Collection<UUID> ids);
    Optional<AssessmentCandidateJpaEntity> findByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);
    List<AssessmentCandidateJpaEntity> findByStudentId(UUID studentId);
    List<AssessmentCandidateJpaEntity> findByScheduleId(UUID scheduleId);
    List<AssessmentCandidateJpaEntity> findByAssignedPaperId(UUID paperId);
    long countByAssessmentId(UUID assessmentId);
    boolean existsByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);

    interface AssessmentIdCandidateCount {
        UUID getAssessmentId();
        long getCandidateCount();
    }

    @Query("""
        SELECT c.assessmentId AS assessmentId, COUNT(c) AS candidateCount
        FROM AssessmentCandidateJpaEntity c
        WHERE c.assessmentId IN :assessmentIds
        GROUP BY c.assessmentId
    """)
    List<AssessmentIdCandidateCount> countByAssessmentIdIn(@Param("assessmentIds") Collection<UUID> assessmentIds);

    @Query("SELECT c.studentId FROM AssessmentCandidateJpaEntity c WHERE c.assessmentId = :assessmentId")
    List<UUID> findStudentIdsByAssessmentId(UUID assessmentId);

    @Query("""
        SELECT DISTINCT c.studentId FROM AssessmentCandidateJpaEntity c
        WHERE c.assessmentId = :assessmentId AND c.blockedAt IS NULL
        ORDER BY c.studentId
        """)
    List<UUID> findDistinctUnblockedStudentIdsByAssessmentId(@Param("assessmentId") UUID assessmentId);
    List<AssessmentCandidateJpaEntity> findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(UUID assessmentId);
    List<AssessmentCandidateJpaEntity> findByIdIn(Collection<UUID> ids);
    List<AssessmentCandidateJpaEntity> findByIdInAndAssessmentId(Collection<UUID> ids, UUID assessmentId);
    long countByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);
    boolean existsByAssessmentIdAndScheduleIdIsNotNull(UUID assessmentId);
    Optional<AssessmentCandidateJpaEntity> findByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);

    @Query("""
        SELECT c
            FROM AssessmentCandidateJpaEntity c
        JOIN AssessmentScheduleJpaEntity s
            ON c.scheduleId = s.id
        WHERE c.studentId = :userId
            AND s.startDate <= :now
            AND s.endDate > :now
    """)
    List<AssessmentCandidateJpaEntity> findActiveCandidate(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Với mỗi học sinh trong {@code studentIds}, ca thi (DRAFT/PUBLISHED) mà họ đã được xếp và giao
     * thời gian với [start, end).
     *
     * <p>Giờ nằm ở bảng cha nên phải join sang {@code assessment_schedules}. Điều kiện giao khoảng dùng
     * đúng dạng nửa mở như {@code SpringDataAssessmentScheduleProctorRepository.countOverlappingAssignments}:
     * hai ca kề nhau (ca trước kết thúc đúng lúc ca sau bắt đầu) KHÔNG tính là trùng.
     *
     * <p>Thí sinh đã miễn thi hoặc đã huỷ không vào phòng nên không chiếm chỗ -- cùng cách phân loại
     * với {@code AssessmentCandidateStatus.isNonScorable}. Trạng thái lưu dạng chuỗi nên phải liệt kê
     * literal ở đây; đổi enum thì phải sửa cả hai chỗ.
     */
    @Query("""
        SELECT c.studentId, s.id, s.startDate, s.endDate
        FROM AssessmentCandidateJpaEntity c
        JOIN AssessmentScheduleJpaEntity s ON s.id = c.scheduleId
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

