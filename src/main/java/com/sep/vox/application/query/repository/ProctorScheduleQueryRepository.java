package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.ProctorScheduleSummary;

public interface ProctorScheduleQueryRepository {
    List<ProctorScheduleSummary> findByTeacherId(UUID teacherId);
    List<ProctorScheduleSummary> findBySchoolId(UUID schoolId);
}
