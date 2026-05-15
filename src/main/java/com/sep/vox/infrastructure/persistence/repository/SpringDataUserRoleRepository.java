package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.UserRoleJpaEntity;

public interface SpringDataUserRoleRepository extends JpaRepository<UserRoleJpaEntity, Long> {
    List<UserRoleJpaEntity> findByUserId(UUID userId);
    List<UserRoleJpaEntity> findByRoleId(UUID roleId);
}
