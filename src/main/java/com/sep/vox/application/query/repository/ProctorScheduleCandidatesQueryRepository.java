package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.ProctorCandidateSummary;

public interface ProctorScheduleCandidatesQueryRepository {
    List<ProctorCandidateSummary> findByScheduleId(UUID scheduleId);
}
