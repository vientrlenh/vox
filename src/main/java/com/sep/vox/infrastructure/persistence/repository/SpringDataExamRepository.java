package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;

public interface SpringDataExamRepository extends JpaRepository<ExamJpaEntity, UUID> {
    List<ExamJpaEntity> findByIdIn(Collection<UUID> ids);
}
