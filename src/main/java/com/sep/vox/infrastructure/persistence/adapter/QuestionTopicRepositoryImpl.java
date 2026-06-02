package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.QuestionTopic;
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
        var entity = QuestionTopicMapper.toJpa(questionTopic);
        var saved = springDataQuestionTopicRepository.save(entity);
        return QuestionTopicMapper.toDomain(saved);
    }

    @Override
    public Optional<QuestionTopic> findById(UUID id) {
        return springDataQuestionTopicRepository.findById(id)
            .map(QuestionTopicMapper::toDomain);
    }

    @Override
    public List<QuestionTopic> findByQuestionBankId(UUID questionBankId) {
        return springDataQuestionTopicRepository.findByQuestionBankId(questionBankId).stream()
            .map(QuestionTopicMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<QuestionTopic> findByQuestionBankId(UUID questionBankId, PageRequest pageRequest) {
        var pageable = org.springframework.data.domain.PageRequest.of(pageRequest.page() - 1, pageRequest.size());
        var page = springDataQuestionTopicRepository.findByQuestionBankId(questionBankId, pageable);
        return new PageResult<>(
            page.getContent().stream()
                .map(QuestionTopicMapper::toDomain)
                .toList(),
            page.getNumber() + 1,
            page.getSize(),
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
}
