package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.model.questionbank.QuestionBank;

public class QuestionBankDtoMapper {

    public static QuestionBankDto toDto(QuestionBank domain) {
        return new QuestionBankDto(
            domain.getId(),
            domain.getBankName(),
            domain.getDescription(),
            domain.isActive(),
            domain.getCreatedAt() != null ? domain.getCreatedAt().toString() : null,
            domain.getUpdatedAt() != null ? domain.getUpdatedAt().toString() : null,
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    public static List<QuestionBankDto> toDtoList(List<QuestionBank> list) {
        return list.stream()
            .map(QuestionBankDtoMapper::toDto)
            .toList();
    }

    public static PageResult<QuestionBankDto> toDtoPage(PageResult<QuestionBank> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
