package com.sep.vox.application.query.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.query.dto.NearestCentralizedExamDto;

public interface NearestCentralizedExamQueryRepository {
    Optional<NearestCentralizedExamDto> findNearestForSchool(UUID schoolId);
}
