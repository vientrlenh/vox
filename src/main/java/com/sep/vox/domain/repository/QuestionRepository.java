package com.sep.vox.domain.repository;

import java.util.Collection;
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
    List<Question> saveAll(Collection<Question> questions);
    Optional<Question> findById(UUID id);
    /**
     * Lookup thẳng theo id, KHÔNG check quyền — chỉ dùng ở những nơi quyền xem đã được
     * xác nhận qua parent (vd: đã pass hasAccess của blueprint/exam chứa câu hỏi này rồi).
     */
    List<Question> findByIdIn(Collection<UUID> ids);
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
    PageResult<Question> findAccessibleForExamPaper(
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
    List<Question> findAccessibleByIdIn(
        java.util.Collection<UUID> ids,
        UUID currentUserId,
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin
    );
    boolean existsUsedInExam(UUID id);
    boolean existsById(UUID id);
    boolean existsByQuestionBankIdAndCode(UUID questionBankId, String code);
    boolean existsByQuestionBankIdAndQuestionTopicIdAndQuestionText(
        UUID questionBankId,
        UUID questionTopicId,
        String questionText
    );
    void deleteById(UUID id);
    List<Question> findBySecurePoolId(UUID securePoolId);
    List<Question> findByQuestionBankId(UUID questionBankId);
    List<Question> findByQuestionTopicId(UUID questionTopicId);
    boolean existsPublishedAndUsedByQuestionBankId(UUID questionBankId);
    boolean existsPublishedAndUsedByQuestionTopicId(UUID questionTopicId);
}
