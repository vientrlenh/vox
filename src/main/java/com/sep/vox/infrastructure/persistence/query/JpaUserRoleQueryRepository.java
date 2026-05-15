package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
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
                ur.user_id,
                ur.role_id,
                ur.created_at,
                r.code,
                r.name
            )
            FROM UserRoleJpaEntity ur 
            JOIN RoleJpaEntity r 
                ON ur.role_id = r.id
            WHERE ur.user_id = :userId
        """, UserRoleInfo.class)
        .setParameter("userId", userId)
        .getResultStream()
        .toList();
    }
    
}
