package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClassUser;

public interface SchoolClassUserRepository {
    Optional<SchoolClassUser> findByUserIdAndSchoolClassId(UUID userId, UUID schoolClassId);
    List<SchoolClassUser> findByUserIdInAndSchoolClassIdIn(Collection<UUID> userIds, Collection<UUID> schoolClassIds);
    List<SchoolClassUser> findByUserId(UUID userId);
    List<SchoolClassUser> findByUserIdIn(Collection<UUID> userIds);
    PageResult<SchoolClassUser> findBySchoolClassId(UUID schoolClassId, int pageNumber, int size);
    boolean existsBySchoolClassId(UUID schoolClassId);
    SchoolClassUser save(SchoolClassUser schoolClassUser);
    List<SchoolClassUser> saveAll(Collection<SchoolClassUser> schoolClassUsers);
}
