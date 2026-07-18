package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.question.QuestionCollaborator;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.infrastructure.persistence.mapper.QuestionCollaboratorMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataQuestionCollaboratorRepository;

@Repository
public class QuestionCollaboratorRepositoryImpl implements QuestionCollaboratorRepository {

    private final SpringDataQuestionCollaboratorRepository springDataQuestionCollaboratorRepository;

    public QuestionCollaboratorRepositoryImpl(SpringDataQuestionCollaboratorRepository springDataQuestionCollaboratorRepository) {
        this.springDataQuestionCollaboratorRepository = springDataQuestionCollaboratorRepository;
    }

    @Override
    public QuestionCollaborator save(QuestionCollaborator collaborator) {
        var entity = QuestionCollaboratorMapper.toJpa(collaborator);
        var saved = springDataQuestionCollaboratorRepository.save(entity);
        return QuestionCollaboratorMapper.toDomain(saved);
    }

    @Override
    public Optional<QuestionCollaborator> findById(UUID id) {
        return springDataQuestionCollaboratorRepository.findById(id)
            .map(QuestionCollaboratorMapper::toDomain);
    }

    @Override
    public Optional<QuestionCollaborator> findByQuestionIdAndUserId(UUID questionId, UUID userId) {
        return springDataQuestionCollaboratorRepository.findByQuestionIdAndUserId(questionId, userId)
            .map(QuestionCollaboratorMapper::toDomain);
    }

    @Override
    public List<QuestionCollaborator> findByQuestionId(UUID questionId) {
        return springDataQuestionCollaboratorRepository.findByQuestionId(questionId).stream()
            .map(QuestionCollaboratorMapper::toDomain)
            .toList();
    }

    @Override
    public List<QuestionCollaborator> findByQuestionIdIn(Collection<UUID> questionIds) {
        if (questionIds.isEmpty()) {
            return List.of();
        }
        return springDataQuestionCollaboratorRepository.findByQuestionIdIn(questionIds).stream()
            .map(QuestionCollaboratorMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataQuestionCollaboratorRepository.deleteById(id);
    }

    @Override
    public void deleteByQuestionId(UUID questionId) {
        springDataQuestionCollaboratorRepository.deleteByQuestionId(questionId);
    }
}
