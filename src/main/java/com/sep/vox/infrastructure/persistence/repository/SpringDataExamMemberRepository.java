package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamMemberJpaEntity;

public interface SpringDataExamMemberRepository extends JpaRepository<ExamMemberJpaEntity, UUID> {
    boolean existsByExamIdAndUserIdAndRole(UUID examId, UUID userId, String role);
}
