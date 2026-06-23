package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.SchoolUserJpaEntity;
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
            WHERE su.school_id IN (:schoolIds)
        ) ranked 
        WHERE ranked.rn BETWEEN :fromRow AND :toRow 
        ORDER BY ranked.school_id, ranked.rn
    """, nativeQuery = true)
    List<SchoolUserJpaEntity> findBySchoolIdIn(@Param("schoolIds") Collection<UUID> schoolIds, @Param("fromRow") int fromRow, @Param("toRow") int toRow);

    Page<SchoolUserJpaEntity> findBySchoolId(UUID schoolId, Pageable pageable);
    Optional<SchoolUserJpaEntity> findBySchoolIdAndUserId(UUID schoolId, UUID userId);
    boolean existsBySchoolIdAndUserId(UUID schoolId, UUID userId);

    @Query(value = """
        SELECT su FROM SchoolUserJpaEntity su
        JOIN UserJpaEntity u ON u.id = su.userId
        WHERE su.schoolId = :schoolId
            AND (:search IS NULL OR LOWER(u.fullName) LIKE :search OR LOWER(u.email) LIKE :search OR LOWER(u.phone) LIKE :search)
            AND ((:status IS NULL AND u.status <> 'DISABLED') OR u.status = :status)
            AND EXISTS (
                SELECT 1 FROM UserRoleJpaEntity ur
                JOIN RoleJpaEntity r ON r.id = ur.roleId
                WHERE ur.userId = u.id
                    AND r.code IN :schoolRoleCodes
                    AND (:roleCode IS NULL OR r.code = :roleCode))
        ORDER BY su.id DESC
        """,
        countQuery = """
        SELECT COUNT(su) FROM SchoolUserJpaEntity su
        JOIN UserJpaEntity u ON u.id = su.userId
        WHERE su.schoolId = :schoolId
            AND (:search IS NULL OR LOWER(u.fullName) LIKE :search OR LOWER(u.email) LIKE :search OR LOWER(u.phone) LIKE :search)
            AND ((:status IS NULL AND u.status <> 'DISABLED') OR u.status = :status)
            AND EXISTS (
                SELECT 1 FROM UserRoleJpaEntity ur
                JOIN RoleJpaEntity r ON r.id = ur.roleId
                WHERE ur.userId = u.id
                    AND r.code IN :schoolRoleCodes
                    AND (:roleCode IS NULL OR r.code = :roleCode))
        """)
    Page<SchoolUserJpaEntity> searchBySchoolId(
        @Param("schoolId") UUID schoolId,
        @Param("search") String search,
        @Param("roleCode") String roleCode,
        @Param("status") String status,
        @Param("schoolRoleCodes") Collection<String> schoolRoleCodes,
        Pageable pageable);
}
