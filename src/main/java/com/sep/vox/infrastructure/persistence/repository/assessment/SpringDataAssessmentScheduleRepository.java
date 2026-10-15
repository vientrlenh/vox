package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentScheduleJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataAssessmentScheduleRepository extends JpaRepository<AssessmentScheduleJpaEntity, UUID> {

  List<AssessmentScheduleJpaEntity> findByExamIdAndStatusNot(UUID examId, String status);

  List<AssessmentScheduleJpaEntity> findByExamIdInAndStatusNot(java.util.Collection<UUID> examIds, String status);

  List<AssessmentScheduleJpaEntity> findByIdIn(java.util.Collection<UUID> ids);

  List<AssessmentScheduleJpaEntity> findByIdInAndStatusNot(java.util.Collection<UUID> ids, String status);

  @Query("SELECT s.id FROM AssessmentScheduleJpaEntity s WHERE s.examId = :examId")
  List<UUID> findAllIdsByExamId(@Param("examId") UUID examId);

  void deleteByExamId(UUID examId);

  @Query("""
              SELECT s FROM AssessmentScheduleJpaEntity s
              WHERE s.examId = :examId
                      AND s.status = 'PUBLISHED'
                      AND s.startDate <= :now AND s.endDate > :now
      """)
  List<AssessmentScheduleJpaEntity> findByExamIdAndInSchedule(
      @Param("examId") UUID examId,
      @Param("now") Instant now);

  @Query("""
      SELECT s FROM AssessmentScheduleJpaEntity s
      WHERE s.status = 'PUBLISHED' AND s.endDate <= :now
      ORDER BY s.endDate ASC
      """)
  List<AssessmentScheduleJpaEntity> findPublishedEndedBefore(@Param("now") Instant now, Pageable pageable);

  @Query("""
      SELECT s FROM AssessmentScheduleJpaEntity s
      WHERE s.status <> 'DELETED'
        AND s.examId IN (SELECT e.id FROM AssessmentJpaEntity e WHERE e.schoolId = :schoolId)
      """)
  List<AssessmentScheduleJpaEntity> findBySchoolId(@Param("schoolId") UUID schoolId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM AssessmentScheduleJpaEntity s WHERE s.id = :id")
  Optional<AssessmentScheduleJpaEntity> findByIdForUpdate(@Param("id") UUID id);

  @Query("""
      SELECT COUNT(s) FROM AssessmentScheduleJpaEntity s
      WHERE s.schoolRoomId = :schoolRoomId
        AND s.status IN ('DRAFT', 'PUBLISHED')
        AND (:excludeScheduleId IS NULL OR s.id <> :excludeScheduleId)
        AND s.startDate < :end AND s.endDate > :start
      """)
  long countOverlapping(
      @Param("schoolRoomId") UUID schoolRoomId,
      @Param("start") Instant start,
      @Param("end") Instant end,
      @Param("excludeScheduleId") UUID excludeScheduleId);

  @Modifying
  @Query("""
      UPDATE AssessmentScheduleJpaEntity s SET
        s.schoolRoomId = COALESCE(:schoolRoomId, s.schoolRoomId),
        s.startDate = COALESCE(:start, s.startDate),
        s.endDate = COALESCE(:end, s.endDate),
        s.updatedAt = :now,
        s.updatedBy = :updatedBy
      WHERE s.id = :id AND s.status = 'DRAFT'
      """)
  int updateAtomic(
      @Param("id") UUID id,
      @Param("schoolRoomId") UUID schoolRoomId,
      @Param("start") Instant start,
      @Param("end") Instant end,
      @Param("now") Instant now,
      @Param("updatedBy") UUID updatedBy);

  @Query("""
      SELECT s FROM AssessmentScheduleJpaEntity s
      WHERE s.id = :id
        AND s.startDate <= :now
        AND s.endDate > :now
        AND s.status = 'PUBLISHED'
      """)
  Optional<AssessmentScheduleJpaEntity> findByIdAndInSchedule(@Param("id") UUID id, @Param("now") Instant now);

  @Query("""
      SELECT s FROM AssessmentScheduleJpaEntity s
      WHERE s.id IN :ids
        AND s.startDate <= :now
        AND s.endDate > :now
        AND s.status = 'PUBLISHED'
      """)
  List<AssessmentScheduleJpaEntity> findByIdInAndInSchedule(@Param("ids") Collection<UUID> ids,
      @Param("now") Instant now);

  @Query("""
      SELECT s FROM AssessmentScheduleJpaEntity s 
      JOIN AssessmentJpaEntity e 
        ON s.examId = e.id 
      WHERE s.id IN :ids
        AND s.startDate <= :now
        AND s.endDate > :now
        AND s.status = 'PUBLISHED' 
        AND e.schoolId = :schoolId 
      """)
  List<AssessmentScheduleJpaEntity> findByIdInAndInScheduleAndSchoolId(@Param("ids") Collection<UUID> ids,
      @Param("now") Instant now, @Param("schoolId") UUID schoolId);
}
