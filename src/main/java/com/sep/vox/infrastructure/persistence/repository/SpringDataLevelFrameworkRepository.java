package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.LevelFrameworkJpaEntity;

public interface SpringDataLevelFrameworkRepository extends JpaRepository<LevelFrameworkJpaEntity, UUID>{
    
}
