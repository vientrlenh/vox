package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamSecurePoolJpaEntity;

public interface SpringDataExamSecurePoolRepository extends JpaRepository<ExamSecurePoolJpaEntity, UUID> {
    Optional<ExamSecurePoolJpaEntity> findByExamId(UUID examId);
    List<ExamSecurePoolJpaEntity> findByExamIdIn(Collection<UUID> examIds);
    void deleteByExamId(UUID examId);
}
