package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.Question;

public interface QuestionRepository {
    Question save(Question question);
    Optional<Question> findById(UUID id);
    List<Question> findByTopicId(UUID topicId);
    PageResult<Question> findByTopicId(UUID topicId, int pageNumber, int size);
    PageResult<Question> findAll(int pageNumber, int size);
    boolean existsById(UUID id);
}
