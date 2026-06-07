package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.infrastructure.persistence.entity.SchoolJpaEntity;

public interface SchoolRoomRepository {
    Optional<SchoolRoom> findById(UUID id);
    SchoolRoom save(SchoolRoom room);
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);

    Optional<SchoolRoom> findByIdForUpdate(UUID id);

    PageResult<SchoolRoom> findAllBySchoolId(UUID schoolId, PageRequest pageRequest);
}
