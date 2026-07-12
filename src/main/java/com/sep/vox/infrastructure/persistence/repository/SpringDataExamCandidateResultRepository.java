package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamCandidateResultJpaEntity;

public interface SpringDataExamCandidateResultRepository extends JpaRepository<ExamCandidateResultJpaEntity, UUID> {
    Optional<ExamCandidateResultJpaEntity> findBySessionId(UUID sessionId);
    List<ExamCandidateResultJpaEntity> findBySessionIdIn(Collection<UUID> sessionIds);
    void deleteBySessionId(UUID sessionId);
}
