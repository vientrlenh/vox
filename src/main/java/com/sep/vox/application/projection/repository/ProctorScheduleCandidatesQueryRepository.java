package com.sep.vox.application.projection.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.projection.dto.ProctorCandidateSummary;

public interface ProctorScheduleCandidatesQueryRepository {
    List<ProctorCandidateSummary> findByScheduleId(UUID scheduleId);
}
