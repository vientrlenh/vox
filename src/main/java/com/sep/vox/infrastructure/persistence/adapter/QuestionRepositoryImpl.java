package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.Question;
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
    public List<Question> findByTopicId(UUID topicId) {
        return springDataQuestionRepository.findByTopicId(topicId).stream()
            .map(QuestionMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<Question> findByTopicId(UUID topicId, PageRequest pageRequest) {
        var pageable = org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        var page = springDataQuestionRepository.findByTopicId(topicId, pageable);
        return new PageResult<>(
            page.getContent().stream()
                .map(QuestionMapper::toDomain)
                .toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    public PageResult<Question> findAll(PageRequest pageRequest) {
        var pageable = org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        var page = springDataQuestionRepository.findAll(pageable);
        return new PageResult<>(
            page.getContent().stream()
                .map(QuestionMapper::toDomain)
                .toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataQuestionRepository.existsById(id);
    }
}
