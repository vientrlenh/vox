package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;

public interface QuestionTopicRepository {
    QuestionTopic save(QuestionTopic questionTopic);
    Optional<QuestionTopic> findById(UUID id);
    List<QuestionTopic> findByQuestionBankId(UUID bankId);
    PageResult<QuestionTopic> findByQuestionBankId(UUID bankId, int pageNumber, int size);
    PageResult<QuestionTopic> findAccessible(
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin,
        UUID questionBankId,
        QuestionTopicStatus status,
        String keyword,
        int pageNumber,
        int size
    );
    boolean existsById(UUID id);
    boolean isTopicBelongToSchool(UUID id, UUID schoolId);
}
