package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamScheduleJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataExamScheduleRepository extends JpaRepository<ExamScheduleJpaEntity, UUID> {

  List<ExamScheduleJpaEntity> findByExamIdAndStatusNot(UUID examId, String status);

  List<ExamScheduleJpaEntity> findByExamIdInAndStatusNot(java.util.Collection<UUID> examIds, String status);

  List<ExamScheduleJpaEntity> findByIdIn(java.util.Collection<UUID> ids);

  List<ExamScheduleJpaEntity> findByIdInAndStatusNot(java.util.Collection<UUID> ids, String status);

  @Query("SELECT s.id FROM ExamScheduleJpaEntity s WHERE s.examId = :examId")
  List<UUID> findAllIdsByExamId(@Param("examId") UUID examId);

  void deleteByExamId(UUID examId);

  @Query("""
              SELECT s FROM ExamScheduleJpaEntity s
              WHERE s.examId = :examId
                      AND s.status = 'PUBLISHED'
                      AND s.startDate <= :now AND s.endDate > :now
      """)
  List<ExamScheduleJpaEntity> findByExamIdAndInSchedule(
      @Param("examId") UUID examId,
      @Param("now") Instant now);

  @Query("""
      SELECT s FROM ExamScheduleJpaEntity s
      WHERE s.status <> 'DELETED'
        AND s.examId IN (SELECT e.id FROM ExamJpaEntity e WHERE e.schoolId = :schoolId)
      """)
  List<ExamScheduleJpaEntity> findBySchoolId(@Param("schoolId") UUID schoolId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM ExamScheduleJpaEntity s WHERE s.id = :id")
  Optional<ExamScheduleJpaEntity> findByIdForUpdate(@Param("id") UUID id);

  @Query("""
      SELECT COUNT(s) FROM ExamScheduleJpaEntity s
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
      UPDATE ExamScheduleJpaEntity s SET
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
      SELECT s FROM ExamScheduleJpaEntity s
      WHERE s.id = :id
        AND s.startDate <= :now
        AND s.endDate > :now
        AND s.status = 'PUBLISHED'
      """)
  Optional<ExamScheduleJpaEntity> findByIdAndInSchedule(@Param("id") UUID id, @Param("now") Instant now);

  @Query("""
      SELECT s FROM ExamScheduleJpaEntity s
      WHERE s.id IN :ids
        AND s.startDate <= :now
        AND s.endDate > :now
        AND s.status = 'PUBLISHED'
      """)
  List<ExamScheduleJpaEntity> findByIdInAndInSchedule(@Param("ids") Collection<UUID> ids,
      @Param("now") Instant now);

  @Query("""
      SELECT s FROM ExamScheduleJpaEntity s 
      JOIN ExamJpaEntity e 
        ON s.examId = e.id 
      WHERE s.id IN :ids
        AND s.startDate <= :now
        AND s.endDate > :now
        AND s.status = 'PUBLISHED' 
        AND e.schoolId = :schoolId 
      """)
  List<ExamScheduleJpaEntity> findByIdInAndInScheduleAndSchoolId(@Param("ids") Collection<UUID> ids,
      @Param("now") Instant now, @Param("schoolId") UUID schoolId);
}
