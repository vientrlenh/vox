package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.infrastructure.persistence.entity.SchoolJpaEntity;

public interface SchoolRoomRepository {
    Optional<SchoolRoom> findById(UUID id);
    SchoolRoom save(SchoolRoom room);
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);

    PageResult<SchoolRoom> findBySchoolId(UUID schoolId, int page, int size);
    Optional<SchoolRoom> findByIdForUpdate(UUID id);
}
