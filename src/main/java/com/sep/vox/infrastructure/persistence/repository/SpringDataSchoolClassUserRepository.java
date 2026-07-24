package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolClassUserJpaEntity;

public interface SpringDataSchoolClassUserRepository extends JpaRepository<SchoolClassUserJpaEntity, UUID>{
    Optional<SchoolClassUserJpaEntity> findByUserIdAndSchoolClassId(UUID userId, UUID schoolClassId);
    List<SchoolClassUserJpaEntity> findByUserIdInAndSchoolClassIdIn(Collection<UUID> userIds, Collection<UUID> schoolClassIds);
    List<SchoolClassUserJpaEntity> findByUserId(UUID userId);
    List<SchoolClassUserJpaEntity> findByUserIdIn(Collection<UUID> userIds);
    Page<SchoolClassUserJpaEntity> findBySchoolClassId(UUID schoolClassId, Pageable pageable);
    boolean existsBySchoolClassId(UUID schoolClassId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SchoolClassUserJpaEntity m
        SET m.isActive = false,
            m.leftAt = :leftAt
        WHERE m.isActive = true
            AND m.schoolClassId IN (
                SELECT c.id FROM SchoolClassJpaEntity c WHERE c.schoolGradeId = :schoolGradeId
            )
        """)
    int deactivateByGradeId(
        @Param("schoolGradeId") UUID schoolGradeId,
        @Param("leftAt") OffsetDateTime leftAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SchoolClassUserJpaEntity m
        SET m.isActive = false,
            m.leftAt = :leftAt
        WHERE m.isActive = true
            AND m.schoolClassId = :schoolClassId
        """)
    int deactivateBySchoolClassId(
        @Param("schoolClassId") UUID schoolClassId,
        @Param("leftAt") OffsetDateTime leftAt
    );
}
