package com.sep.vox.infrastructure.persistence.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaUserRoleQueryRepository implements UserRoleQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<UserRoleInfo> findByUserIdWithRoleInfo(UUID userId) {
        return em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.UserRoleInfo(
                ur.id,
                ur.userId,
                ur.roleId,
                ur.createdAt,
                r.code,
                r.name
            )
            FROM UserRoleJpaEntity ur
            JOIN RoleJpaEntity r
                ON ur.roleId = r.id
            WHERE ur.userId = :userId
        """, UserRoleInfo.class)
        .setParameter("userId", userId)
        .getResultStream()
        .toList();
    }

    @Override
    public Set<UUID> findUserIdsByRoleCode(Collection<UUID> userIds, String roleCode) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(em.createQuery("""
            SELECT DISTINCT ur.userId
            FROM UserRoleJpaEntity ur
            JOIN RoleJpaEntity r ON ur.roleId = r.id
            WHERE ur.userId IN :userIds AND r.code = :roleCode
        """, UUID.class)
        .setParameter("userIds", userIds)
        .setParameter("roleCode", roleCode)
        .getResultList());
    }

}
