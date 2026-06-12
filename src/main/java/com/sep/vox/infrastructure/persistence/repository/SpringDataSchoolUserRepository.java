package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolUserJpaEntity;

public interface SpringDataSchoolUserRepository extends JpaRepository<SchoolUserJpaEntity, UUID>{
    Optional<SchoolUserJpaEntity> findByUserId(UUID userId);
    List<SchoolUserJpaEntity> findByUserIdIn(Collection<UUID> userIds);
}
