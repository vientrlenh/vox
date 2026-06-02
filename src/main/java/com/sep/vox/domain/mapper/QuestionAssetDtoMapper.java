package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.model.question.QuestionAsset;

public class QuestionAssetDtoMapper {

    public static QuestionAssetDto toDto(QuestionAsset domain) {
        return new QuestionAssetDto(
            domain.getId(),
            domain.getQuestionId(),
            domain.getTitle(),
            domain.getDurationSeconds(),
            domain.getAltText(),
            domain.getType().name(),
            domain.getUrl(),
            domain.getTranscript(),
            domain.getDescription(),
            domain.getOrder()
        );
    }

    public static List<QuestionAssetDto> toDtoList(List<QuestionAsset> list) {
        return list.stream()
            .map(QuestionAssetDtoMapper::toDto)
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
