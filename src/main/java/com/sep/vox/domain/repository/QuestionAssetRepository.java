package com.sep.vox.domain.repository;

import java.util.List;

import com.sep.vox.domain.model.question.QuestionAsset;

public interface QuestionAssetRepository {
    QuestionAsset save(QuestionAsset questionAsset);
    List<QuestionAsset> saveAll(List<QuestionAsset> questionAssets);
}
