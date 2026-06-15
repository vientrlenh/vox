package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionAsset;

public interface QuestionAssetRepository {
    QuestionAsset save(QuestionAsset questionAsset);
    List<QuestionAsset> saveAll(List<QuestionAsset> questionAssets);
    List<QuestionAsset> findByQuestionId(UUID questionId);
    void deleteById(UUID id);
    void deleteByQuestionId(UUID questionId);
    void flush();
}
