package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolLevelJpaEntity;

public interface SpringDataSchoolLevelRepository extends JpaRepository<SchoolLevelJpaEntity, UUID>{
    
}
