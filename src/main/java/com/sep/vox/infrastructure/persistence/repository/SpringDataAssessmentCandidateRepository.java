package com.sep.vox.infrastructure.persistence.repository;

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

    @Query(value = """
        SELECT c.*
        FROM assessment_candidates c
        JOIN (
            SELECT id, ROW_NUMBER() OVER (PARTITION BY assessment_id ORDER BY assigned_at, id) AS position_in_assessment
            FROM assessment_candidates
            WHERE assessment_id IN (:assessmentIds)
        ) ranked ON ranked.id = c.id
        WHERE ranked.position_in_assessment = 1
        """, nativeQuery = true)
    List<AssessmentCandidateJpaEntity> findFirstCandidateByAssessmentIdIn(@Param("assessmentIds") Collection<UUID> assessmentIds);

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
     * For each student in {@code studentIds}, the sessions (DRAFT/PUBLISHED) they have been placed in whose time
     * intersects [start, end).
     *
     * <p>It now lives on the parent table so it has to join to {@code assessment_schedules}. The interval intersection condition uses
     * exactly the half-open form of {@code SpringDataAssessmentScheduleProctorRepository.countOverlappingAssignments}:
     * two adjacent sessions (the earlier one ending exactly when the later one starts) do NOT count as overlapping.
     *
     * <p>A candidate who is exempted or cancelled does not enter the room so does not take a seat -- the same classification
     * as {@code AssessmentCandidateStatus.isNonScorable}. The status is stored as a string so the
     * literals have to be listed here; if the enum changes both places must be edited.
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

