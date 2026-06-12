package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolUser;

public interface SchoolUserRepository {
    Optional<SchoolUser> findByUserId(UUID userId);
    SchoolUser save(SchoolUser schoolUser);
}
