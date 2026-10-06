package com.sep.vox.application.projection.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.projection.dto.SchoolUserInfo;
import com.sep.vox.domain.common.PageResult;

public interface SchoolUserProjectionRepository {
    Optional<UUID> findSchoolIdByUserId(UUID userId);
    PageResult<SchoolUserInfo> findBySchoolIdAndRoleCodes(UUID schoolId, List<String> roleCodes, int page, int size);
}
