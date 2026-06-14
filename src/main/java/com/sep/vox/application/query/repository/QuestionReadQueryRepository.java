package com.sep.vox.application.query.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;

public interface QuestionReadQueryRepository {

    // Common
    Optional<QuestionDto> findVisibleQuestion(UUID questionId, UUID userId, String role, UUID schoolId);

    // Teacher - QuestionController
    PageResult<QuestionDto> findTeacherMyQuestions(UUID userId, PageRequest page);
    PageResult<QuestionDto> findTeacherVisibleQuestions(UUID userId, UUID schoolId, String scope, String status, String type, String keyword, PageRequest page);
    PageResult<QuestionDto> findTeacherReviewQueue(UUID userId, UUID schoolId, PageRequest page);

    // School - QuestionController
    PageResult<QuestionDto> findSchoolVisibleQuestions(UUID schoolId, String scope, String status, String type, String keyword, PageRequest page);
    PageResult<QuestionDto> findSchoolReviewQueue(UUID schoolId, PageRequest page);

    // Admin - QuestionController
    PageResult<QuestionDto> findAdminQuestions(Boolean includeArchived, String status, String keyword, PageRequest page);
    PageResult<QuestionDto> findAdminReviewQueue(PageRequest page);

    // Admin - QuestionBankController
    PageResult<QuestionDto> findAdminBankQuestions(UUID bankId, Boolean includeArchived, String scope, String status, String type, String keyword, PageRequest page);
}
