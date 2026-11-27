package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;

public interface QuestionBankRepository {
    QuestionBank save(QuestionBank questionBank);
    Optional<QuestionBank> findById(UUID id);
    List<QuestionBank> findByIdIn(Collection<UUID> ids);
    PageResult<QuestionBank> findAll(int pageNumber, int size);
    PageResult<QuestionBank> findAccessible(
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin,
        QuestionBankOwnerType ownerType,
        QuestionBankStatus status,
        UUID languageId,
        UUID schoolId,
        UUID schoolGradeId,
        String keyword,
        int pageNumber,
        int size
    );
    /**
     * Question banks in exactly one ownership scope, so import can check for duplicate {@code code}.
     *
     * <p>{@code schoolId} must be {@code null} when {@code ownerType} is {@code SYSTEM} — a system bank
     * belongs to no school. Do not use {@code findAccessible} for this: that function paginates and mixes in access rights,
     * while here the full list of ONE scope is needed.
     */
    List<QuestionBank> findByOwnerScope(QuestionBankOwnerType ownerType, UUID schoolId);

    boolean existsById(UUID id);
    void deleteById(UUID id);
}
