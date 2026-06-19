package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.School;

public interface SchoolRepository {
    Optional<School> findById(UUID id);
    Optional<School> findByCode(String code);
    Optional<School> findByDomain(String domain);
    PageResult<School> findAll(int page, int size);
    School save(School school);
    boolean existsById(UUID id);
    boolean existsByDomain(String domain);
    List<School> findByIdIn(Collection<UUID> ids, int page, int size);
    boolean existsByIdAndIsActiveTrue(UUID schoolId);
    List<School> findByIdIn(Collection<UUID> ids);
    boolean existsByCode(String code);
}
