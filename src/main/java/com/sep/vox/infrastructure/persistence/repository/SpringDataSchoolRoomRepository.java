package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolRoomJpaEntity;

public interface SpringDataSchoolRoomRepository extends JpaRepository<SchoolRoomJpaEntity, UUID> {
    
}
