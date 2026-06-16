package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolUser;

public interface SchoolUserRepository {
    Optional<SchoolUser> findByUserId(UUID userId);
    List<SchoolUser> findByUserIdIn(Collection<UUID> userIds);
    SchoolUser save(SchoolUser schoolUser);
    List<SchoolUser> findBySchoolIdIn(Collection<UUID> schoolIds, int page, int size);
    PageResult<SchoolUser> findBySchoolId(UUID schoolId, int page, int size);
    Optional<SchoolUser> findBySchoolIdAndUserId(UUID schoolId, UUID userId);
    Optional<UUID> findSchoolIdByUserId(UUID userId);
}
