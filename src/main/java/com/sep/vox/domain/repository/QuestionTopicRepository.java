package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.questiontopic.QuestionTopic;

public interface QuestionTopicRepository {
    QuestionTopic save(QuestionTopic questionTopic);
    Optional<QuestionTopic> findById(UUID id);
    List<QuestionTopic> findByBankId(UUID bankId);
    PageResult<QuestionTopic> findByBankId(UUID bankId, PageRequest pageRequest);
    boolean existsById(UUID id);
}
