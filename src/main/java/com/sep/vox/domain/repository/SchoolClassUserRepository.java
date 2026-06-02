package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.schoolclassuser.SchoolClassUser;

public interface SchoolClassUserRepository {
    Optional<SchoolClassUser> findByUserIdAndSchoolClassId(UUID userId, UUID schoolClassId);
    List<SchoolClassUser> findByUserId(UUID userId);
    List<SchoolClassUser> findBySchoolClassId(UUID schoolClassId);
    boolean existsBySchoolClassId(UUID schoolClassId);
    SchoolClassUser save(SchoolClassUser schoolClassUser);
}
