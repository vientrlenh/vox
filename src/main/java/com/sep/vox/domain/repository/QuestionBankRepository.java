package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;

public interface QuestionBankRepository {
    QuestionBank save(QuestionBank questionBank);
    Optional<QuestionBank> findById(UUID id);
    PageResult<QuestionBank> findAll(int pageNumber, int size);
    PageResult<QuestionBank> findAccessible(
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin,
        QuestionBankOwnerType ownerType,
        QuestionBankStatus status,
        UUID languageId,
        UUID schoolId,
        String keyword,
        int pageNumber,
        int size
    );
    boolean existsById(UUID id);
    void deleteById(UUID id);
}
