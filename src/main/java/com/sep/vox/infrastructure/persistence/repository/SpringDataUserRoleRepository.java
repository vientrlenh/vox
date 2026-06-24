package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import com.sep.vox.infrastructure.persistence.entity.UserRoleJpaEntity;

public interface SpringDataUserRoleRepository extends JpaRepository<UserRoleJpaEntity, UUID> {
    List<UserRoleJpaEntity> findByUserId(UUID userId);
    List<UserRoleJpaEntity> findByRoleId(UUID roleId);
    boolean existsByRoleId(UUID roleId);
    Optional<UserRoleJpaEntity> findByUserIdAndRoleId(UUID userId, UUID roleId);

    List<UserRoleJpaEntity> findByUserIdIn(Collection<UUID> userIds);

    @Query(value = """
        SELECT 
            id, 
            user_id, 
            role_id, 
            created_at  
        FROM (
            SELECT 
                ur.*, 
                row_number() OVER (
                    PARTITION BY ur.role_id
                    ORDER BY ur.id DESC
                ) AS rn
            FROM user_roles ur 
            WHERE ur.role_id IN (:roleIds) 
        ) ranked 
        WHERE ranked.rn BETWEEN :fromRow AND :toRow
        ORDER BY ranked.role_id, ranked.rn
    """, nativeQuery = true)
    List<UserRoleJpaEntity> findByRoleIdIn(@Param("roleIds") Collection<UUID> roleIds, @Param("fromRow") int fromRow, @Param("toRow") int toRow);
}
