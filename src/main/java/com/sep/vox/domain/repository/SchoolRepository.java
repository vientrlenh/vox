package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.School;

public interface SchoolRepository {
    Optional<School> findById(UUID id);
    Optional<School> findByCode(String code);
    Optional<School> findByDomain(String domain);
    PageResult<School> findAll(PageRequest pageRequest);
    School save(School school);
    boolean existsById(UUID id);
}
