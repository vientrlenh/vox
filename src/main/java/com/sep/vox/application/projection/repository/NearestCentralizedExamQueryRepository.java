package com.sep.vox.application.projection.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.projection.dto.NearestCentralizedExamDto;

public interface NearestCentralizedExamQueryRepository {
    Optional<NearestCentralizedExamDto> findNearestForSchool(UUID schoolId);
}
