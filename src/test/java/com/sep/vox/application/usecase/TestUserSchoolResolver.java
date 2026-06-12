package com.sep.vox.application.usecase;

import com.sep.vox.application.usecase.TestUserSchoolResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.service.UserSchoolResolver;

public class TestUserSchoolResolver extends UserSchoolResolver {

    private static final Map<UUID, UUID> SCHOOLS_BY_USER = new HashMap<>();

    private TestUserSchoolResolver() {
        super(null, null);
    }

    public static TestUserSchoolResolver create() {
        SCHOOLS_BY_USER.clear();
        return new TestUserSchoolResolver();
    }

    public static void remember(UUID userId, UUID schoolId) {
        if (userId != null && schoolId != null) {
            SCHOOLS_BY_USER.put(userId, schoolId);
        }
    }

    @Override
    public Optional<UUID> findSchoolId(UUID userId) {
        return Optional.ofNullable(SCHOOLS_BY_USER.get(userId));
    }
}
