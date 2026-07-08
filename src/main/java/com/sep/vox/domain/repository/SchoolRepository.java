package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
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
    PageResult<School> findAll(int pageNumber, int size);
    School save(School school);
    boolean existsById(UUID id);
    boolean existsByDomain(String domain);


    boolean existsByDomainAndIdNot(String domain, UUID id);
    boolean existsByContactEmailAndIdNot(String email, UUID id);
    boolean existsByContactPhoneAndIdNot(String phone, UUID id);

    void deleteById(UUID id);

    boolean existsByIdAndIsActiveTrue(UUID schoolId);

    int updateSchoolAtomic(UUID id, String name, String description, String phone,
                           String email, String domain, String address, Integer studentCount,
                           OffsetDateTime now, UUID updatedBy);
    List<School> findByIdIn(Collection<UUID> ids);
    boolean existsByCode(String code);
}
