package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.School;

public interface SchoolRepository {
    Optional<School> findById(UUID id);
    Optional<School> findByCode(String code);

    /**
     * Returns a LIST rather than an Optional: the domain name is NO LONGER the identity of a school, a
     * school with several campuses shares 1 domain name and each campus is its own School. Going back to Optional
     * means one day NonUniqueResultException blows up somewhere unrelated.
     */
    List<School> findByDomain(String domain);
    PageResult<School> findAll(int pageNumber, int size, String search, Boolean isActive);
    School save(School school);
    boolean existsById(UUID id);

    boolean existsByContactEmailAndIdNot(String email, UUID id);
    boolean existsByContactPhoneAndIdNot(String phone, UUID id);

    void deleteById(UUID id);

    boolean existsByIdAndIsActiveTrue(UUID schoolId);

    int updateSchoolAtomic(UUID id, String name, String description, boolean descriptionProvided, String phone,
                           String email, String domain, boolean domainProvided, String address, Integer studentCount);
    List<School> findByIdIn(Collection<UUID> ids);
    boolean existsByCode(String code);

    long countAll();
    long countByIsActiveTrue();

    /** The ids of schools that currently have a session running -- the same predicate as AssessmentSchedule.isOngoingAt. */
    List<UUID> findIdsWithOngoingAssessment(Instant now);
}
