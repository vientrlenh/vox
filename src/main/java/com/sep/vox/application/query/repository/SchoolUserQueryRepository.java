package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.SchoolUserInfo;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;

public interface SchoolUserQueryRepository {
    PageResult<SchoolUserInfo> findBySchoolIdAndRoleCodes(UUID schoolId, List<String> roleCodes, PageRequest pageRequest);
}
