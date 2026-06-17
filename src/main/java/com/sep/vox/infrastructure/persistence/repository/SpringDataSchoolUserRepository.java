package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolUserJpaEntity;

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
}
