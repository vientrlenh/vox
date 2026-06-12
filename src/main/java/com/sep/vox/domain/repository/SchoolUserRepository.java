package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolUser;

public interface SchoolUserRepository {
    Optional<SchoolUser> findByUserId(UUID userId);
    List<SchoolUser> findByUserIdIn(Collection<UUID> userIds);
    SchoolUser save(SchoolUser schoolUser);
}
