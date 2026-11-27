package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkVersionJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataFrameworkVersionRepository extends JpaRepository<FrameworkVersionJpaEntity, UUID> {
    boolean existsByFrameworkId(UUID frameworkId);
    Optional<FrameworkVersionJpaEntity> findByCode(String code);
    Optional<FrameworkVersionJpaEntity> findByName(String name);
    // Receives input already uppercased by the Impl; the entity side still wraps UPPER() in case old data has inconsistent case.
    @Query("SELECT v FROM FrameworkVersionJpaEntity v WHERE UPPER(v.code) IN :codes")
    List<FrameworkVersionJpaEntity> findByCodeIn(@Param("codes") Collection<String> codes);
    List<FrameworkVersionJpaEntity> findByNameIn(Collection<String> names);
    Page<FrameworkVersionJpaEntity> findByFrameworkId(UUID frameworkId, Pageable pageable);
    List<FrameworkVersionJpaEntity> findByFrameworkIdAndStatus(UUID frameworkId, String status);
    Page<FrameworkVersionJpaEntity> findByFrameworkIdAndStatus(UUID frameworkId, String status, Pageable pageable);
    Optional<FrameworkVersionJpaEntity> findByFrameworkIdAndVersion(UUID frameworkId, int version);

    /**
     * Every active assessment framework together with its LATEST published VERSION -- the source for the framework
     * picker on the practice screen.
     *
     * <p>{@code DISTINCT ON (f.id)} plus {@code ORDER BY f.id, fv.version DESC} gives exactly one
     * row per framework, the one with the highest version. Same validity conditions as
     * {@code findActiveVersionId} -- the two paths must be consistent, otherwise the screen lists
     * a framework that the other place would not pick.
     *
     * <p>Differs from {@code findActiveVersionId}: that one returns EXACTLY ONE framework for the whole system, while
     * this one returns ALL of them so the student can choose.
     */
    @Query(value = """
        SELECT DISTINCT ON (f.id)
               fv.id            AS versionId,
               fv.code          AS versionCode,
               fv.version       AS versionNumber,
               f.id             AS frameworkId,
               f.code           AS frameworkCode,
               f.name           AS frameworkName,
               f.description    AS frameworkDescription
        FROM framework_versions fv
        JOIN frameworks f ON f.id = fv.framework_id AND f.is_active = true
        WHERE fv.status = 'PUBLISHED'
          AND fv.effective_from <= CURRENT_TIMESTAMP
          AND (fv.effective_to IS NULL OR fv.effective_to >= CURRENT_TIMESTAMP)
        ORDER BY f.id, fv.version DESC
        """, nativeQuery = true)
    List<ActiveFrameworkProjection> findActiveFrameworks();

    interface ActiveFrameworkProjection {
        UUID getVersionId();
        String getVersionCode();
        Integer getVersionNumber();
        UUID getFrameworkId();
        String getFrameworkCode();
        String getFrameworkName();
        String getFrameworkDescription();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM FrameworkVersionJpaEntity v WHERE v.id = :id")
    Optional<FrameworkVersionJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE FrameworkVersionJpaEntity v SET v.status = :status WHERE v.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") String status);

    /**
     * The framework version currently in force for the WHOLE SYSTEM, used for practice.
     *
     * <p>Why it does not go through {@code assessment_policies} like the assessment path: a policy is chosen by
     * the class the student belongs to (priority class &gt; grade &gt; school). Three consequences we do not want in
     * practice -- a school that has not configured a policy leaves the student unable to enter a session; two students
     * of the same school in different classes practice under two different scales; and a student in several active
     * classes gets whichever policy wins by ordering rather than by intent.
     *
     * <p>{@code frameworks} are not tied to a school or a language, so this is a system-wide concept -- one
     * lookup answers it, with no scope ambiguity.
     *
     * <p>{@code :code} allows pinning directly through config
     * ({@code app.practice.framework-version-code}); when null the active version is picked automatically.
     * This path exists so that a school with several frameworks does not depend on the {@code is_active} flag.
     */
    @Query(value = """
        SELECT fv.id
        FROM framework_versions fv
        JOIN frameworks f ON f.id = fv.framework_id AND f.is_active = true
        WHERE fv.status = 'PUBLISHED'
          AND fv.effective_from <= CURRENT_TIMESTAMP
          AND (fv.effective_to IS NULL OR fv.effective_to >= CURRENT_TIMESTAMP)
          AND (CAST(:code AS varchar) IS NULL OR fv.code = CAST(:code AS varchar))
        ORDER BY fv.version DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<UUID> findActiveVersionId(@Param("code") String code);
}
