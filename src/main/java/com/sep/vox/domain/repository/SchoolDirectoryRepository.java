package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolDirectory;

public interface SchoolDirectoryRepository {
    Optional<SchoolDirectory> findById(UUID id);
    boolean existsByCode(String code);
    boolean existsById(UUID id);
    SchoolDirectory save(SchoolDirectory sd);
    List<SchoolDirectory> findByCodeIn(Collection<String> codes);
    List<SchoolDirectory> findAllByOrderByIdAsc(int limit);
    List<SchoolDirectory> findByIdGreaterThanOrderByIdAsc(UUID cursor, int limit);
    PageResult<SchoolDirectory> findAll(int pageNumber, int size);
}
