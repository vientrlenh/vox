package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
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

    // Lọc vai trò bằng EXISTS chứ không JOIN role: user mang nhiều vai trò sẽ bị
    // nhân dòng nếu JOIN, làm sai cả nội dung trang lẫn totalElements.
    @Query("""
        SELECT scu
        FROM SchoolClassUserJpaEntity scu
        JOIN UserJpaEntity u ON u.id = scu.userId
        WHERE scu.schoolClassId = :schoolClassId
            AND (:searchPattern IS NULL
                OR LOWER(u.fullName) LIKE :searchPattern
                OR LOWER(u.email) LIKE :searchPattern)
            AND (:roleCode IS NULL OR EXISTS (
                SELECT 1
                FROM UserRoleJpaEntity ur
                JOIN RoleJpaEntity r ON r.id = ur.roleId
                WHERE ur.userId = u.id
                    AND r.code = :roleCode))
        ORDER BY u.fullName ASC, scu.id ASC
        """)
    Page<SchoolClassUserJpaEntity> findBySchoolClassIdWithFilters(
        @Param("schoolClassId") UUID schoolClassId,
        @Param("roleCode") String roleCode,
        @Param("searchPattern") String searchPattern,
        Pageable pageable);

    @Query("""
        SELECT scu.schoolClassId, COUNT(scu.id)
        FROM SchoolClassUserJpaEntity scu
        WHERE scu.schoolClassId IN :schoolClassIds
            AND scu.isActive = true
        GROUP BY scu.schoolClassId
        """)
    List<Object[]> countActiveBySchoolClassIdIn(@Param("schoolClassIds") Collection<UUID> schoolClassIds);

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
        @Param("leftAt") Instant leftAt
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
        @Param("leftAt") Instant leftAt
    );
}
