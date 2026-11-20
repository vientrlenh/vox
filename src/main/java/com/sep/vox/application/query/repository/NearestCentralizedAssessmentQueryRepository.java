package com.sep.vox.application.query.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.query.dto.NearestCentralizedAssessmentDto;

public interface NearestCentralizedAssessmentQueryRepository {
    Optional<NearestCentralizedAssessmentDto> findNearestForSchool(UUID schoolId);
}
