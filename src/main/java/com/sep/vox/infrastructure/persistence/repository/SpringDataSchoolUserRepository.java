package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.school.SchoolUserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface SpringDataSchoolUserRepository extends JpaRepository<SchoolUserJpaEntity, UUID>{
    Optional<SchoolUserJpaEntity> findByUserId(UUID userId);
    List<SchoolUserJpaEntity> findByUserIdIn(Collection<UUID> userIds);

    @Query("SELECT su.schoolId FROM SchoolUserJpaEntity su WHERE su.userId = :userId")
    Optional<UUID> findSchoolIdByUserId(@Param("userId") UUID userId);



    @Query(value = """
        SELECT 
            id, 
            school_id, 
            user_id, 
            start_date, 
            end_date 
        FROM (
            SELECT
                su.*,
                row_number() OVER (
                    PARTITION BY su.school_id
                    ORDER BY su.id DESC
                ) AS rn
            FROM school_users su
            JOIN users u ON u.id = su.user_id
            WHERE su.school_id IN (:schoolIds)
                AND u.status <> 'DISABLED'
        ) ranked 
        WHERE ranked.rn BETWEEN :fromRow AND :toRow 
        ORDER BY ranked.school_id, ranked.rn
    """, nativeQuery = true)
    List<SchoolUserJpaEntity> findBySchoolIdIn(@Param("schoolIds") Collection<UUID> schoolIds, @Param("fromRow") int fromRow, @Param("toRow") int toRow);

    Optional<SchoolUserJpaEntity> findBySchoolIdAndUserId(UUID schoolId, UUID userId);
    boolean existsBySchoolIdAndUserId(UUID schoolId, UUID userId);

    /**
     * {@code excludeAdminRoles} removes administrators (SCHOOL_ADMIN/SYSTEM_ADMIN) from the result: the school's
     * user-picking screens (class assignment, teacher/student lists) never
     * act on administrator accounts.
     */
    @Query(value = """
        SELECT su FROM SchoolUserJpaEntity su
        JOIN UserJpaEntity u ON u.id = su.userId
        WHERE su.schoolId = :schoolId
            AND (:excludeUserId IS NULL OR su.userId <> :excludeUserId)
            AND (:search IS NULL OR LOWER(u.fullName) LIKE :search OR LOWER(u.email) LIKE :search OR LOWER(u.phone) LIKE :search)
            AND ((:status IS NULL AND u.status <> 'DISABLED') OR u.status = :status)
            AND (:roleId IS NULL OR EXISTS (
                SELECT 1 FROM UserRoleJpaEntity ur
                WHERE ur.userId = u.id
                    AND ur.roleId = :roleId))
            AND (:excludeAdminRoles = FALSE OR NOT EXISTS (
                SELECT 1 FROM UserRoleJpaEntity adminUserRole
                JOIN RoleJpaEntity adminRole ON adminRole.id = adminUserRole.roleId
                WHERE adminUserRole.userId = u.id
                    AND adminRole.code IN ('SCHOOL_ADMIN', 'SYSTEM_ADMIN')))
            AND (:excludeClassId IS NULL OR NOT EXISTS (
                SELECT 1 FROM SchoolClassUserJpaEntity scu
                WHERE scu.userId = u.id
                    AND scu.schoolClassId = :excludeClassId
                    AND scu.isActive = true))
        ORDER BY su.id DESC
        """,
        countQuery = """
        SELECT COUNT(su) FROM SchoolUserJpaEntity su
        JOIN UserJpaEntity u ON u.id = su.userId
        WHERE su.schoolId = :schoolId
            AND (:excludeUserId IS NULL OR su.userId <> :excludeUserId)
            AND (:search IS NULL OR LOWER(u.fullName) LIKE :search OR LOWER(u.email) LIKE :search OR LOWER(u.phone) LIKE :search)
            AND ((:status IS NULL AND u.status <> 'DISABLED') OR u.status = :status)
            AND (:roleId IS NULL OR EXISTS (
                SELECT 1 FROM UserRoleJpaEntity ur
                WHERE ur.userId = u.id
                    AND ur.roleId = :roleId))
            AND (:excludeAdminRoles = FALSE OR NOT EXISTS (
                SELECT 1 FROM UserRoleJpaEntity adminUserRole
                JOIN RoleJpaEntity adminRole ON adminRole.id = adminUserRole.roleId
                WHERE adminUserRole.userId = u.id
                    AND adminRole.code IN ('SCHOOL_ADMIN', 'SYSTEM_ADMIN')))
            AND (:excludeClassId IS NULL OR NOT EXISTS (
                SELECT 1 FROM SchoolClassUserJpaEntity scu
                WHERE scu.userId = u.id
                    AND scu.schoolClassId = :excludeClassId
                    AND scu.isActive = true))
        """)
    Page<SchoolUserJpaEntity> searchBySchoolId(
        @Param("schoolId") UUID schoolId,
        @Param("excludeUserId") UUID excludeUserId,
        @Param("search") String search,
        @Param("roleId") UUID roleId,
        @Param("status") String status,
        @Param("excludeClassId") UUID excludeClassId,
        @Param("excludeAdminRoles") boolean excludeAdminRoles,
        Pageable pageable);


    /**
     * DISTINCT is required: the three JOINs here multiply rows when a person is assigned the same role
     * several times. Without it the recipient list in the outbox payload contains duplicate UUIDs.
     */
    @Query("""
        SELECT DISTINCT su FROM SchoolUserJpaEntity su
        JOIN UserRoleJpaEntity ur
            ON su.userId = ur.userId
        JOIN UserJpaEntity u
            ON su.userId = u.id
        JOIN RoleJpaEntity r
            ON ur.roleId = r.id
        WHERE su.schoolId = :schoolId
            AND r.code = :roleCode
            AND u.status <> 'DISABLED'
    """)
    List<SchoolUserJpaEntity> findBySchoolIdWithRole(@Param("schoolId") UUID schoolId, @Param("roleCode") String roleCode);
}
