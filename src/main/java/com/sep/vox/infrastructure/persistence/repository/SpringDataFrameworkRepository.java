package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.FrameworkJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface SpringDataFrameworkRepository extends JpaRepository<FrameworkJpaEntity, UUID> {
    Optional<FrameworkJpaEntity> findByCode(String code);
    List<FrameworkJpaEntity> findByIdIn(Collection<UUID> ids);

}
