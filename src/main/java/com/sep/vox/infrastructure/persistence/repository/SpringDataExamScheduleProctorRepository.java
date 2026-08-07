package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamScheduleProctorJpaEntity;

public interface SpringDataExamScheduleProctorRepository extends JpaRepository<ExamScheduleProctorJpaEntity, UUID> {
    List<ExamScheduleProctorJpaEntity> findByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndTeacherId(UUID scheduleId, UUID teacherId);
    long countByScheduleId(UUID scheduleId);

    List<ExamScheduleProctorJpaEntity> findByTeacherIdAndScheduleIdIn(UUID teacherId, Collection<UUID> scheduleIds);
    void deleteByScheduleIdIn(Collection<UUID> scheduleIds);
}
