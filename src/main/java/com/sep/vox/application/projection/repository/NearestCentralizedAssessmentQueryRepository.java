package com.sep.vox.application.projection.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.projection.dto.NearestCentralizedAssessmentDto;

public interface NearestCentralizedAssessmentQueryRepository {
    Optional<NearestCentralizedAssessmentDto> findNearestForSchool(UUID schoolId);
}
