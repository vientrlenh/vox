package com.sep.vox.application.query.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;

public interface QuestionReadQueryRepository {

    Optional<QuestionDto> findVisibleQuestion(UUID questionId, UUID userId, String role, UUID schoolId);

    PageResult<QuestionDto> findTeacherMyQuestions(UUID userId, PageRequest page);

    PageResult<QuestionDto> findTeacherVisibleQuestions(
            UUID userId,
            UUID schoolId,
            String scope,
            String status,
            String type,
            String keyword,
            PageRequest page);

    PageResult<QuestionDto> findTeacherReviewQueue(UUID userId, UUID schoolId, PageRequest page);

    PageResult<QuestionDto> findSchoolVisibleQuestions(
            UUID schoolId,
            String scope,
            String status,
            String type,
            String keyword,
            PageRequest page);

    PageResult<QuestionDto> findSchoolReviewQueue(UUID schoolId, PageRequest page);

    PageResult<QuestionDto> findAdminQuestions(
            UUID userId,
            Boolean includeArchived,
            String status,
            String keyword,
            PageRequest page);

    PageResult<QuestionDto> findAdminReviewQueue(UUID userId, PageRequest page);

    PageResult<QuestionDto> findAdminTopicQuestions(
            UUID bankId,
            UUID topicId,
            UUID userId,
            Boolean includeArchived,
            String scope,
            String status,
            String type,
            String keyword,
            PageRequest page);

    PageResult<QuestionDto> findAdminBankQuestions(
            UUID bankId,
            UUID userId,
            Boolean includeArchived,
            String scope,
            String status,
            String type,
            String keyword,
            PageRequest page);
}
