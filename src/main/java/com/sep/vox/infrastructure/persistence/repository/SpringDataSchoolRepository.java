package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.school.SchoolJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSchoolRepository extends JpaRepository<SchoolJpaEntity, UUID> {
    Optional<SchoolJpaEntity> findByCode(String code);

    List<SchoolJpaEntity> findByDomain(String domain);

    boolean existsByCode(String code);

    boolean existsByContactEmail(String contactEmail);

    boolean existsByContactPhone(String contactPhone);

    boolean existsByContactEmailAndIdNot(String email, UUID id);
    boolean existsByContactPhoneAndIdNot(String phone, UUID id);

    boolean existsByCodeAndIdNot(String normalizedCode, UUID id);
    boolean existsByIdAndIsActiveTrue(UUID schoolId);

    // COALESCE works as follows: if the given name is null, the stored s.name is kept.
    // description and domain are nullable columns, so their "Provided" flags decide between keeping and clearing them.
    @Modifying
    @Query("""
            UPDATE SchoolJpaEntity s SET 
            s.name = COALESCE(:name, s.name),
            s.description = CASE WHEN :descriptionProvided = true THEN :description ELSE s.description END,
            s.contactPhone = COALESCE(:phone, s.contactPhone),
            s.contactEmail = COALESCE(:email, s.contactEmail),
            s.domain = CASE WHEN :domainProvided = true THEN :domain ELSE s.domain END,
            s.address = COALESCE(:address, s.address),
            s.studentCount = COALESCE(:studentCount, s.studentCount),
            s.updatedAt = :updatedAt,
            s.updatedBy = :updatedBy
            WHERE s.id = :id
            """)
    int updateSchoolAtomic(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("description") String description,
            @Param("descriptionProvided") boolean descriptionProvided,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("domain") String domain,
            @Param("domainProvided") boolean domainProvided,
            @Param("address") String address,
            @Param("studentCount") Integer studentCount,
            @Param("updatedAt") Instant updatedAt,
            @Param("updatedBy") String updatedBy
    );
    List<SchoolJpaEntity> findByIdIn(Collection<UUID> ids);

    long countByIsActiveTrue();

    @Query("SELECT s FROM SchoolJpaEntity s WHERE " +
           "(:search IS NULL OR LOWER(s.name) LIKE :search ESCAPE '!' OR LOWER(s.code) LIKE :search ESCAPE '!')")
    Page<SchoolJpaEntity> findAllBySearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT s FROM SchoolJpaEntity s WHERE " +
           "(:search IS NULL OR LOWER(s.name) LIKE :search ESCAPE '!' OR LOWER(s.code) LIKE :search ESCAPE '!') " +
           "AND s.isActive = :isActive")
    Page<SchoolJpaEntity> findAllBySearchAndIsActive(@Param("search") String search, @Param("isActive") boolean isActive, Pageable pageable);

    /**
     * Schools that currently have a session RUNNING: published, its time has come, not yet ended.
     *
     * <p>It checks the SCHEDULE and not the open attempt sessions, because this must be EXACTLY the predicate that
     * {@code ForceSuspendSubscriptionUseCase} uses to refuse a suspension -- it calls
     * {@code AssessmentSchedule.isOngoingAt}, i.e. the status of the SCHEDULE. Counting by attempt session (status =
     * 'IN_PROGRESS') would miss exactly the most common case: the assessment door has just opened but no student has clicked in yet. Then
     * the BE still blocks but this list is empty, so the button shows enabled, the admin clicks, and only then gets an error -- exactly
     * what the blocking state exists to avoid.
     *
     * <p>The same expression as {@code SpringDataAssessmentScheduleRepository.findByAssessmentIdAndInSchedule}, only
     * grouped by school instead of by one assessment.
     */
    @Query("""
        SELECT DISTINCT assessment.schoolId
        FROM AssessmentScheduleJpaEntity schedule
        JOIN AssessmentJpaEntity assessment
            ON schedule.assessmentId = assessment.id
        WHERE schedule.status = 'PUBLISHED'
            AND schedule.startDate <= :now
            AND schedule.endDate > :now
        """)
    List<UUID> findIdsWithOngoingAssessment(@Param("now") Instant now);
}
