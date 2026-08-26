package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolRoom;

public interface SchoolRoomRepository {
    Optional<SchoolRoom> findById(UUID id);
    List<SchoolRoom> findByIdIn(Collection<UUID> ids);
    SchoolRoom save(SchoolRoom room);
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);
    List<SchoolRoom> findBySchoolIdAndCodeIn(UUID schoolId, Collection<String> codes);


    PageResult<SchoolRoom> findBySchoolId(UUID schoolId, int pageNumber, int size);

    boolean existsBySchoolIdAndIsActive(UUID schoolId, boolean isActive);
    int updateSchoolRoomAtomic(UUID id, String name, String description, Instant now, UUID updatedBy);
}
