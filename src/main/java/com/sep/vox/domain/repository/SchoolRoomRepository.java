package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolRoom;

public interface SchoolRoomRepository {
    Optional<SchoolRoom> findById(UUID id);
    SchoolRoom save(SchoolRoom room);
}
