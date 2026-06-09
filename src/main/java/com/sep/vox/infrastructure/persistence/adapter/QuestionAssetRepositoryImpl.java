package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.infrastructure.persistence.mapper.QuestionAssetMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataQuestionAssetRepository;

@Repository
public class QuestionAssetRepositoryImpl implements QuestionAssetRepository {

    private final SpringDataQuestionAssetRepository springDataQuestionAssetRepository;

    public QuestionAssetRepositoryImpl(SpringDataQuestionAssetRepository springDataQuestionAssetRepository) {
        this.springDataQuestionAssetRepository = springDataQuestionAssetRepository;
    }

    @Override
    public QuestionAsset save(QuestionAsset questionAsset) {
        var entity = QuestionAssetMapper.toJpa(questionAsset);
        var saved = springDataQuestionAssetRepository.save(entity);
        return QuestionAssetMapper.toDomain(saved);
    }

    @Override
    public List<QuestionAsset> saveAll(List<QuestionAsset> questionAssets) {
        var entities = questionAssets.stream()
            .map(QuestionAssetMapper::toJpa)
            .toList();
        return springDataQuestionAssetRepository.saveAll(entities)
            .stream()
            .map(QuestionAssetMapper::toDomain)
            .toList();
    }

    @Override
    public List<QuestionAsset> findByQuestionId(UUID questionId) {
        return springDataQuestionAssetRepository.findByQuestionIdOrderByOrder(questionId)
            .stream()
            .map(QuestionAssetMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByQuestionId(UUID questionId) {
        springDataQuestionAssetRepository.deleteByQuestionId(questionId);
    }
}
