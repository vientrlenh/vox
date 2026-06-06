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
    boolean existsByCode(String code);
    boolean existsByDomain(String domain);
    boolean existsByContactEmail(String contactEmail);
    boolean existsByContactPhone(String contactPhone);

    boolean existsByDomainAndIdNot(String domain, UUID id);
    boolean existsByContactEmailAndIdNot(String email, UUID id);
    boolean existsByContactPhoneAndIdNot(String phone, UUID id);

    void deleteById(UUID id);

    boolean existsByCodeAndIdNot(String normalizedCode, UUID id);
}
