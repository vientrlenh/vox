package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;

public interface QuestionRepository {
    Question save(Question question);
    Optional<Question> findById(UUID id);
    PageResult<Question> findAccessible(
        UUID currentUserId,
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin,
        UUID questionBankId,
        UUID questionTopicId,
        String topicName,
        QuestionStatus status,
        QuestionType type,
        QuestionSharing sharing,
        String scope,
        String keyword,
        int pageNumber,
        int size
    );
    Optional<Question> findAccessibleById(
        UUID id,
        UUID currentUserId,
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin
    );
    boolean existsUsedInExam(UUID id);
    boolean existsById(UUID id);
    void deleteById(UUID id);
    List<Question> findBySecurePoolId(UUID securePoolId);
    List<Question> findByQuestionBankId(UUID questionBankId);
    List<Question> findByQuestionTopicId(UUID questionTopicId);
    boolean existsPublishedAndUsedByQuestionBankId(UUID questionBankId);
    boolean existsPublishedAndUsedByQuestionTopicId(UUID questionTopicId);
}
