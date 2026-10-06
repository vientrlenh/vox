package com.sep.vox.application.projection.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.projection.dto.ProctorScheduleSummary;

public interface ProctorScheduleQueryRepository {
    List<ProctorScheduleSummary> findByTeacherId(UUID teacherId);
    List<ProctorScheduleSummary> findBySchoolId(UUID schoolId);
}
