package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamScheduleProctorJpaEntity;

public interface SpringDataExamScheduleProctorRepository extends JpaRepository<ExamScheduleProctorJpaEntity, UUID>{
    boolean existsByScheduleIdAndTeacherId(UUID scheduleId, UUID teacherId);
}
