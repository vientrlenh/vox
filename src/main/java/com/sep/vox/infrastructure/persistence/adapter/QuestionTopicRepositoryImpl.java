package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.QuestionTopicJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.infrastructure.persistence.mapper.QuestionTopicMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataQuestionTopicRepository;

@Repository
public class QuestionTopicRepositoryImpl implements QuestionTopicRepository {

    private final SpringDataQuestionTopicRepository springDataQuestionTopicRepository;

    public QuestionTopicRepositoryImpl(SpringDataQuestionTopicRepository springDataQuestionTopicRepository) {
        this.springDataQuestionTopicRepository = springDataQuestionTopicRepository;
    }

    @Override
    public QuestionTopic save(QuestionTopic questionTopic) {
        QuestionTopicJpaEntity entity = QuestionTopicMapper.toJpa(questionTopic);
        QuestionTopicJpaEntity saved = springDataQuestionTopicRepository.save(entity);
        return QuestionTopicMapper.toDomain(saved);
    }

    @Override
    public Optional<QuestionTopic> findById(UUID id) {
        return springDataQuestionTopicRepository.findById(id)
            .map(QuestionTopicMapper::toDomain);
    }

    @Override
    public List<QuestionTopic> findByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataQuestionTopicRepository.findAllById(ids).stream()
            .map(QuestionTopicMapper::toDomain)
            .toList();
    }

    @Override
    public List<QuestionTopic> findByQuestionBankId(UUID questionBankId) {
        return springDataQuestionTopicRepository.findByQuestionBankId(questionBankId).stream()
            .map(QuestionTopicMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<QuestionTopic> findByQuestionBankId(UUID questionBankId, int pageNumber, int size) {
        PageRequest pageable = PageRequest.of(pageNumber - 1, size);
        Page<QuestionTopicJpaEntity> page = springDataQuestionTopicRepository.findByQuestionBankId(questionBankId, pageable);
        return new PageResult<>(
            page.getContent().stream()
                .map(QuestionTopicMapper::toDomain)
                .toList(),
            pageNumber,
            size,
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataQuestionTopicRepository.existsById(id);
    }

    @Override
    public boolean isTopicBelongToSchool(UUID id, UUID schoolId) {
        return springDataQuestionTopicRepository.isTopicBelongToSchool(id, schoolId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataQuestionTopicRepository.deleteById(id);
    }

    @Override
    public PageResult<QuestionTopic> findAccessible(UUID currentSchoolId, boolean systemAdmin, boolean schoolAdmin,
            UUID questionBankId, QuestionTopicStatus status, String keyword, int pageNumber, int size) {
        // pageNumber is 1-based like every other paginated query in the project; Spring Data counts from 0.
        //
        // The subscription refactor flipped 35 pagination fields in the GraphQL schema to 1-based
        // but did NOT update the matching adapters, so for a long time the schema promised 1-based
        // while this code still counted from 0: the first page the client asked for came back as the
        // second page. No error surfaced because the data types did not change -- only the data was
        // wrong. This is one of the 16 adapters that were pulled back in line; the shared convention
        // is that an adapter always receives 1-based and subtracts on its own.
        PageRequest pageable = PageRequest.of(pageNumber - 1, size);
        Page<QuestionTopicJpaEntity> page = springDataQuestionTopicRepository.findAccessible(
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            questionBankId,
            status == null ? null : status.name(),
            StringNormalization.toLikePattern(keyword),
            pageable
        );
        return new PageResult<>(
            page.getContent().stream()
                .map(QuestionTopicMapper::toDomain)
                .toList(),
            pageNumber,
            size,
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
