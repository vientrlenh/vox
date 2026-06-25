package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.QuestionBank;

public interface QuestionBankRepository {
    QuestionBank save(QuestionBank questionBank);
    Optional<QuestionBank> findById(UUID id);
    PageResult<QuestionBank> findAll(int pageNumber, int size);
    boolean existsById(UUID id);
}
