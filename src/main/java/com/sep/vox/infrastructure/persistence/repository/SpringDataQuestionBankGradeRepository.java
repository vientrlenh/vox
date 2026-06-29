package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.QuestionBankGradeJpaEntity;

public interface SpringDataQuestionBankGradeRepository extends JpaRepository<QuestionBankGradeJpaEntity, UUID> {
    List<QuestionBankGradeJpaEntity> findByQuestionBankId(UUID questionBankId);
    boolean existsByQuestionBankIdAndSchoolGradeId(UUID questionBankId, UUID schoolGradeId);
    void deleteByQuestionBankId(UUID questionBankId);
}
