package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;

public interface SpringDataExamRepository extends JpaRepository<ExamJpaEntity, UUID> {
    
}
