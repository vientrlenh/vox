package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionBankGrade;

public interface QuestionBankGradeRepository {
    QuestionBankGrade save(QuestionBankGrade grade);
    Optional<QuestionBankGrade> findById(UUID id);
    List<QuestionBankGrade> findByQuestionBankId(UUID questionBankId);
    boolean existsByQuestionBankIdAndSchoolGradeId(UUID questionBankId, UUID schoolGradeId);
    void deleteById(UUID id);
    void deleteByQuestionBankId(UUID questionBankId);
}
