package com.sep.vox.application.query.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;

public interface QuestionBankReadQueryRepository {
    PageResult<QuestionBankDto> findTeacherQuestionBanks(UUID userId, UUID schoolId, PageRequest pageRequest);
    Optional<QuestionBankDto> findTeacherQuestionBank(UUID bankId, UUID userId, UUID schoolId);
    PageResult<QuestionBankDto> findSchoolQuestionBanks(UUID schoolId, PageRequest pageRequest);
    Optional<QuestionBankDto> findSchoolQuestionBank(UUID bankId, UUID schoolId);
    Optional<QuestionBankDto> findAdminQuestionBank(UUID bankId);
}
