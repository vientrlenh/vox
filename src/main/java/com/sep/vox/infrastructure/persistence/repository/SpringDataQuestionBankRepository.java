package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.QuestionBankJpaEntity;

public interface SpringDataQuestionBankRepository extends JpaRepository<QuestionBankJpaEntity, UUID> {
}
