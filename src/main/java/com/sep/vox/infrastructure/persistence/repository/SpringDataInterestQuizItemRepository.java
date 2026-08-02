package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.InterestQuizItemJpaEntity;

public interface SpringDataInterestQuizItemRepository
        extends JpaRepository<InterestQuizItemJpaEntity, UUID> {

    List<InterestQuizItemJpaEntity> findTop7ByActiveTrueOrderById();

    List<InterestQuizItemJpaEntity> findByIdAndActiveTrue(UUID id);

    List<InterestQuizItemJpaEntity> findTop7ByStudentIdAndActiveTrueOrderById(UUID studentId);

    boolean existsByStudentId(UUID studentId);
}
