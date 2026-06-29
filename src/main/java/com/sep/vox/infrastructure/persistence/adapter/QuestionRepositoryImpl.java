package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.infrastructure.persistence.mapper.QuestionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataQuestionRepository;

@Repository
public class QuestionRepositoryImpl implements QuestionRepository {

    private final SpringDataQuestionRepository springDataQuestionRepository;

    public QuestionRepositoryImpl(SpringDataQuestionRepository springDataQuestionRepository) {
        this.springDataQuestionRepository = springDataQuestionRepository;
    }

    @Override
    public Question save(Question question) {
        var entity = QuestionMapper.toJpa(question);
        var saved = springDataQuestionRepository.save(entity);
        return QuestionMapper.toDomain(saved);
    }

    @Override
    public Optional<Question> findById(UUID id) {
        return springDataQuestionRepository.findById(id)
            .map(QuestionMapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataQuestionRepository.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        springDataQuestionRepository.deleteById(id);
    }

    @Override
    public boolean existsUsedInExam(UUID id) {
        return springDataQuestionRepository.existsUsedInExam(id);
    }

    @Override
    public List<Question> findBySecurePoolId(UUID securePoolId) {
        return springDataQuestionRepository.findBySecurePoolId(securePoolId).stream()
            .map(QuestionMapper::toDomain)
            .toList();
    }

    @Override
    public List<Question> findByQuestionBankId(UUID questionBankId) {
        return springDataQuestionRepository.findByQuestionBankId(questionBankId).stream()
            .map(QuestionMapper::toDomain)
            .toList();
    }

    @Override
    public List<Question> findByQuestionTopicId(UUID questionTopicId) {
        return springDataQuestionRepository.findByQuestionTopicId(questionTopicId).stream()
            .map(QuestionMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsPublishedAndUsedByQuestionBankId(UUID questionBankId) {
        return springDataQuestionRepository.existsPublishedAndUsedByQuestionBankId(questionBankId);
    }

    @Override
    public boolean existsPublishedAndUsedByQuestionTopicId(UUID questionTopicId) {
        return springDataQuestionRepository.existsPublishedAndUsedByQuestionTopicId(questionTopicId);
    }

    @Override
    public PageResult<Question> findAccessible(UUID currentUserId, UUID currentSchoolId, boolean systemAdmin,
            boolean schoolAdmin, UUID questionBankId, UUID questionTopicId, String topicName, QuestionStatus status,
            QuestionType type, QuestionSharing sharing, String scope, String keyword, int pageNumber, int size) {
        var pageable = PageRequest.of(pageNumber, size);
        var page = springDataQuestionRepository.findAccessible(
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            questionBankId,
            questionTopicId,
            topicName,
            status == null ? null : status.name(),
            type == null ? null : type.name(),
            sharing == null ? null : sharing.name(),
            scope,
            keyword,
            pageable
        );
        return new PageResult<>(
            page.getContent().stream()
                .map(QuestionMapper::toDomain)
                .toList(),
            pageNumber,
            size,
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    public Optional<Question> findAccessibleById(UUID id, UUID currentUserId, UUID currentSchoolId, boolean systemAdmin,
            boolean schoolAdmin) {
        return springDataQuestionRepository.findAccessibleById(
            id,
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin
        ).map(QuestionMapper::toDomain);
    }
}
