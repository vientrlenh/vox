package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolRoom;

public interface SchoolRoomRepository {
    Optional<SchoolRoom> findById(UUID id);
    SchoolRoom save(SchoolRoom room);
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);


    PageResult<SchoolRoom> findAllBySchoolId(UUID schoolId, int pageNumber, int size);

    boolean existsBySchoolIdAndIsActive(UUID schoolId, boolean isActive);
    int updateSchoolRoomAtomic(UUID id, String name, String description, Integer capacity, OffsetDateTime now, UUID updatedBy);
}
