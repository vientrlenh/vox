package com.sep.vox.application.usecase;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;

public class TestSchoolUserRepository implements SchoolUserRepository {

    private static final Map<UUID, UUID> SCHOOLS_BY_USER = new HashMap<>();

    private TestSchoolUserRepository() {
    }

    public static TestSchoolUserRepository create() {
        SCHOOLS_BY_USER.clear();
        return new TestSchoolUserRepository();
    }

    public static void remember(UUID userId, UUID schoolId) {
        if (userId != null && schoolId != null) {
            SCHOOLS_BY_USER.put(userId, schoolId);
        }
    }

    @Override
    public Optional<SchoolUser> findByUserId(UUID userId) {
        return Optional.ofNullable(SCHOOLS_BY_USER.get(userId))
            .map(schoolId -> new SchoolUser(schoolId, userId, OffsetDateTime.now(), null));
    }

    @Override
    public List<SchoolUser> findByUserIdIn(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream()
            .flatMap(userId -> findByUserId(userId).stream())
            .toList();
    }

    @Override
    public SchoolUser save(SchoolUser schoolUser) {
        remember(schoolUser.getUserId(), schoolUser.getSchoolId());
        return schoolUser;
    }

    @Override
    public List<SchoolUser> findBySchoolIdIn(Collection<UUID> schoolIds, int page, int size) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBySchoolIdIn'");
    }

    @Override
    public PageResult<SchoolUser> findBySchoolId(UUID schoolId, int page, int size) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBySchoolId'");
    }

    @Override
    public Optional<SchoolUser> findBySchoolIdAndUserId(UUID schoolId, UUID userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBySchoolIdAndUserId'");
    }

    @Override
    public boolean existsBySchoolIdAndUserId(UUID schoolId, UUID userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'existsBySchoolIdAndUserId'");
    }

    @Override
    public Optional<UUID> findSchoolIdByUserId(UUID userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findSchoolIdByUserId'");
    }
}
