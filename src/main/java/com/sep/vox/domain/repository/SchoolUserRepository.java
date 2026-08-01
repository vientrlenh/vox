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
    List<SchoolUser> findBySchoolIdIn(Collection<UUID> schoolIds, int pageNumber, int size);
    PageResult<SchoolUser> findBySchoolId(UUID schoolId, String search, UUID roleId, String status,
        UUID excludeClassId, boolean excludeAdminRoles, int pageNumber, int size);
    PageResult<SchoolUser> searchBySchoolId(UUID schoolId, UUID excludeUserId, String search, UUID roleId,
        String status, int pageNumber, int size);
    Optional<SchoolUser> findBySchoolIdAndUserId(UUID schoolId, UUID userId);
    Optional<UUID> findSchoolIdByUserId( UUID userId);
    boolean existsBySchoolIdAndUserId(UUID schoolId, UUID userId);
}
