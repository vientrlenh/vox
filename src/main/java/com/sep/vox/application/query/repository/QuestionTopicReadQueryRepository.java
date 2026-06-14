package com.sep.vox.application.query.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionTopicDto;

public interface QuestionTopicReadQueryRepository {

    PageResult<QuestionTopicDto> findTeacherBankTopics(UUID bankId, UUID userId, UUID schoolId, PageRequest page);
    Optional<QuestionTopicDto> findTeacherTopicDetail(UUID topicId, UUID userId, UUID schoolId);
    PageResult<QuestionDto> findTeacherTopicQuestions(UUID bankId, UUID topicId, UUID userId, UUID schoolId, String scope, String status, String type, String keyword, PageRequest page);

    PageResult<QuestionTopicDto> findSchoolBankTopics(UUID bankId, UUID schoolId, PageRequest page);
    Optional<QuestionTopicDto> findSchoolTopicDetail(UUID topicId, UUID schoolId);
    PageResult<QuestionDto> findSchoolTopicQuestions(UUID bankId, UUID topicId, UUID schoolId, String scope, String status, String type, String keyword, PageRequest page);

    PageResult<QuestionTopicDto> findAdminBankTopics(UUID bankId, Boolean includeArchived, PageRequest page);
    Optional<QuestionTopicDto> findAdminTopicDetail(UUID topicId);
    PageResult<QuestionDto> findAdminTopicQuestions(UUID bankId, UUID topicId, Boolean includeArchived, String scope, String status, String type, String keyword, PageRequest page);
}
