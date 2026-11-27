package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.question.QuestionJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.infrastructure.persistence.mapper.QuestionMapper;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;
import com.sep.vox.infrastructure.persistence.repository.SpringDataQuestionRepository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class QuestionRepositoryImpl implements QuestionRepository {

    private final SpringDataQuestionRepository springDataQuestionRepository;

    @Override
    public Question save(Question question) {
        QuestionJpaEntity entity = QuestionMapper.toJpa(question);
        QuestionJpaEntity saved = springDataQuestionRepository.save(entity);
        return QuestionMapper.toDomain(saved);
    }

    @Override
    public List<Question> saveAll(Collection<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        List<QuestionJpaEntity> entities = questions.stream()
            .map(QuestionMapper::toJpa)
            .toList();
        return springDataQuestionRepository.saveAll(entities).stream()
            .map(QuestionMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Question> findById(UUID id) {
        return springDataQuestionRepository.findById(id)
            .map(QuestionMapper::toDomain);
    }

    @Override
    public List<Question> findByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        long startedAt = System.nanoTime();
        List<Question> result = springDataQuestionRepository.findAllById(ids).stream()
            .map(QuestionMapper::toDomain)
            .toList();
        log.info("[blueprint-perf] repo QuestionRepositoryImpl.findByIdIn ids={} tookMs={}",
            ids.size(), (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataQuestionRepository.existsById(id);
    }

    @Override
    public boolean existsByQuestionBankIdAndCode(UUID questionBankId, String code) {
        return springDataQuestionRepository.existsByQuestionBankIdAndCode(questionBankId, code);
    }

    @Override
    public boolean existsByQuestionBankIdAndQuestionTopicIdAndQuestionText(
            UUID questionBankId,
            UUID questionTopicId,
            String questionText) {
        return springDataQuestionRepository.existsByQuestionBankIdAndQuestionTopicIdAndQuestionText(
            questionBankId,
            questionTopicId,
            questionText
        );
    }

    @Override
    public void deleteById(UUID id) {
        springDataQuestionRepository.deleteById(id);
    }

    @Override
    public boolean existsUsedInAssessment(UUID id) {
        return springDataQuestionRepository.existsUsedInAssessment(id);
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
            QuestionType type, QuestionSharing sharing, String assetType, String scope, String keyword,
            int pageNumber, int size) {
        PageRequest pageable = OneBasedPaging.toPageRequest(pageNumber, size);
        Page<QuestionJpaEntity> page = springDataQuestionRepository.findAccessible(
            currentUserId, currentSchoolId, systemAdmin, schoolAdmin, questionBankId, questionTopicId,
            StringNormalization.toLikePattern(topicName),
            status == null ? null : status.name(),
            type == null ? null : type.name(),
            sharing == null ? null : sharing.name(),
            assetType, scope, StringNormalization.toLikePattern(keyword), pageable
        );
        return toPageResult(page, pageNumber, size);
    }

    @Override
    public PageResult<Question> findAccessibleForAssessmentPaper(UUID currentUserId, UUID currentSchoolId, boolean systemAdmin,
            boolean schoolAdmin, UUID questionBankId, UUID questionTopicId, String topicName, QuestionStatus status,
            QuestionType type, QuestionSharing sharing, String assetType, String scope, String keyword,
            int pageNumber, int size) {
        PageRequest pageable = OneBasedPaging.toPageRequest(pageNumber, size);
        Page<QuestionJpaEntity> page = springDataQuestionRepository.findAccessibleForAssessmentPaper(
            currentUserId, currentSchoolId, systemAdmin, schoolAdmin, questionBankId, questionTopicId,
            StringNormalization.toLikePattern(topicName),
            status == null ? null : status.name(),
            type == null ? null : type.name(),
            sharing == null ? null : sharing.name(),
            assetType, scope, StringNormalization.toLikePattern(keyword), pageable
        );
        return toPageResult(page, pageNumber, size);
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

    @Override
    public List<Question> findAccessibleByIdIn(Collection<UUID> ids, UUID currentUserId, UUID currentSchoolId,
            boolean systemAdmin, boolean schoolAdmin) {
        if (ids.isEmpty()) {
            return List.of();
        }
        long startedAt = System.nanoTime();
        List<Question> result = springDataQuestionRepository.findAccessibleByIdIn(
            ids,
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin
        ).stream().map(QuestionMapper::toDomain).toList();
        log.info("[blueprint-perf] repo QuestionRepositoryImpl.findAccessibleByIdIn ids={} tookMs={}",
            ids.size(), (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    private PageResult<Question> toPageResult(Page<QuestionJpaEntity> page, int pageNumber, int size) {
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
}
