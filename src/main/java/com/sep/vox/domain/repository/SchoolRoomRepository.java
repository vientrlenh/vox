package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.schoolroom.SchoolRoom;

public interface SchoolRoomRepository {
    Optional<SchoolRoom> findById(UUID id);
    SchoolRoom save(SchoolRoom room);
    boolean existsBySchoolIdAndCode(UUID schoolId, String code);
}
