package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.QuestionTopic;

public interface QuestionTopicRepository {
    QuestionTopic save(QuestionTopic questionTopic);
    Optional<QuestionTopic> findById(UUID id);
    void deleteById(UUID id);
    List<QuestionTopic> findByQuestionBankId(UUID bankId);
    PageResult<QuestionTopic> findByQuestionBankId(UUID bankId, PageRequest pageRequest);
    boolean existsById(UUID id);
    boolean isTopicBelongToSchool(UUID id, UUID schoolId);
}
