package com.sep.vox.domain.dto;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.shared.PageResult;

public record QuestionAssetDto(
    UUID id,
    UUID questionId,
    String title,
    Integer durationSeconds,
    String altText,
    QuestionAssetType type,
    String url,
    String transcript,
    String description,
    int order
) {

    public static QuestionAssetDto toDto(QuestionAsset questionAsset) {
        return new QuestionAssetDto(
            questionAsset.getId(),
            questionAsset.getQuestionId(),
            questionAsset.getTitle(),
            questionAsset.getDurationSeconds(),
            questionAsset.getAltText(),
            questionAsset.getType(),
            questionAsset.getUrl(),
            questionAsset.getTranscript(),
            questionAsset.getDescription(),
            questionAsset.getOrder()
        );
    }

    public static List<QuestionAssetDto> toDtoList(List<QuestionAsset> list) {
        return list.stream()
            .map(QuestionAssetDto::toDto)
            .toList();
    }

    public static PageResult<QuestionAssetDto> toDtoPage(PageResult<QuestionAsset> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
