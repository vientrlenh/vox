package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;


import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.infrastructure.persistence.mapper.QuestionBankMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataQuestionBankRepository;

@Repository
public class QuestionBankRepositoryImpl implements QuestionBankRepository {

    private final SpringDataQuestionBankRepository springDataQuestionBankRepository;

    public QuestionBankRepositoryImpl(SpringDataQuestionBankRepository springDataQuestionBankRepository) {
        this.springDataQuestionBankRepository = springDataQuestionBankRepository;
    }

    @Override
    public QuestionBank save(QuestionBank questionBank) {
        var entity = QuestionBankMapper.toJpa(questionBank);
        var saved = springDataQuestionBankRepository.save(entity);
        return QuestionBankMapper.toDomain(saved);
    }

    @Override
    public Optional<QuestionBank> findById(UUID id) {
        return springDataQuestionBankRepository.findById(id)
            .map(QuestionBankMapper::toDomain);
    }

    @Override
    public List<QuestionBank> findByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataQuestionBankRepository.findAllById(ids).stream()
            .map(QuestionBankMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<QuestionBank> findAll(int pageNumber, int size) {
        var pageable = PageRequest.of(pageNumber - 1, size);
        var page = springDataQuestionBankRepository.findAll(pageable);
        return new PageResult<>(
            page.getContent().stream()
                .map(QuestionBankMapper::toDomain)
                .toList(),
            pageNumber,
            size,
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataQuestionBankRepository.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        springDataQuestionBankRepository.deleteById(id);
    }

    @Override
    public PageResult<QuestionBank> findAccessible(UUID currentSchoolId, boolean systemAdmin, boolean schoolAdmin,
            QuestionBankOwnerType ownerType, QuestionBankStatus status, UUID languageId, UUID schoolId,
            UUID schoolGradeId, String keyword, int pageNumber, int size) {
        var pageable = PageRequest.of(pageNumber - 1, size);
        var page = springDataQuestionBankRepository.findAccessible(
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            ownerType == null ? null : ownerType.name(),
            status == null ? null : status.name(),
            languageId,
            schoolId,
            schoolGradeId,
            StringNormalization.toLikePattern(keyword),
            pageable
        );
        return new PageResult<>(
            page.getContent().stream()
                .map(QuestionBankMapper::toDomain)
                .toList(),
            pageNumber,
            size,
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
