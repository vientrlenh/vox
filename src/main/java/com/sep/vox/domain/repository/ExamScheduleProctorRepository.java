package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamScheduleProctor;

public interface ExamScheduleProctorRepository {
    ExamScheduleProctor save(ExamScheduleProctor proctor);
    Optional<ExamScheduleProctor> findById(UUID id);
    List<ExamScheduleProctor> findByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndTeacherId(UUID scheduleId, UUID teacherId);
    long countByScheduleId(UUID scheduleId);
    void deleteById(UUID id);

    void deleteByScheduleIdIn(Collection<UUID> scheduleIds);
    List<UUID> findScheduleIdsByTeacherIdAndScheduleIdIn(UUID teacherId, Collection<UUID> scheduleIds);
}
