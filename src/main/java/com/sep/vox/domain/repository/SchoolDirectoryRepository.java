package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolDirectory;

public interface SchoolDirectoryRepository {
    Optional<SchoolDirectory> findById(UUID id);
    boolean existsByCode(String code);
    boolean existsById(UUID id);
    SchoolDirectory save(SchoolDirectory sd);
}
