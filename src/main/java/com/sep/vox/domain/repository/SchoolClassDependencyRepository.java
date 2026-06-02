package com.sep.vox.domain.repository;

import java.util.UUID;

public interface SchoolClassDependencyRepository {
    boolean existsDependencyBySchoolClassId(UUID schoolClassId);
}
