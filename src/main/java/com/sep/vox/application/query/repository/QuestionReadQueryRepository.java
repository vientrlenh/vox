package com.sep.vox.application.query.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionTopicDto;

public interface QuestionReadQueryRepository {

    // Common
    Optional<QuestionDto> findVisibleQuestion(UUID questionId, UUID userId, String role, UUID schoolId);

    // Teacher - QuestionController
    PageResult<QuestionDto> findTeacherMyQuestions(UUID userId, PageRequest page);
    PageResult<QuestionDto> findTeacherReviewQueue(UUID userId, UUID schoolId, PageRequest page);

    // School - QuestionController
    PageResult<QuestionDto> findSchoolReviewQueue(UUID schoolId, PageRequest page);

    // Admin - QuestionController
    PageResult<QuestionDto> findAdminQuestions(Boolean includeArchived, String status, String keyword, PageRequest page);
    PageResult<QuestionDto> findAdminReviewQueue(PageRequest page);

    // Teacher - QuestionTopicController
    PageResult<QuestionTopicDto> findTeacherBankTopics(UUID bankId, UUID userId, UUID schoolId, PageRequest page);
    PageResult<QuestionDto> findTeacherTopicQuestions(UUID bankId, UUID topicId, UUID userId, UUID schoolId, String status, String keyword, PageRequest page);

    // School - QuestionTopicController
    PageResult<QuestionTopicDto> findSchoolBankTopics(UUID bankId, UUID schoolId, PageRequest page);
    PageResult<QuestionDto> findSchoolTopicQuestions(UUID bankId, UUID topicId, UUID schoolId, String status, String keyword, PageRequest page);

    // Admin - QuestionTopicController
    PageResult<QuestionTopicDto> findAdminBankTopics(UUID bankId, Boolean includeArchived, PageRequest page);

    // Admin - QuestionBankController
    PageResult<QuestionDto> findAdminBankQuestions(UUID bankId, Boolean includeArchived, String status, String keyword, PageRequest page);
}
