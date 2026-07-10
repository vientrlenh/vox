package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.FrameworkJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface SpringDataFrameworkRepository extends JpaRepository<FrameworkJpaEntity, UUID> {
    Optional<FrameworkJpaEntity> findByCode(String code);
    List<FrameworkJpaEntity> findByIdIn(Collection<UUID> ids);

    @Query("SELECT f FROM FrameworkJpaEntity f WHERE f.isActive = true")
    Page<FrameworkJpaEntity> findAllActive(Pageable pageable);

}
