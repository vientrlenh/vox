package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.model.question.QuestionBank;


public class QuestionBankDtoMapper {

    public static QuestionBankDto toDto(QuestionBank domain) {
        return new QuestionBankDto(
            domain.getId(),
            domain.getLanguageId(),
            domain.getSchoolId(),
            domain.getCode(),
            domain.getName(),
            domain.getDescription(),
            domain.getOwnerType().name(),
            domain.getStatus().name(),
            valueOf(domain.getCreatedAt()),
            valueOf(domain.getUpdatedAt()),
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

    private static String valueOf(Instant date) {
        return date == null ? null : date.toString();
    }
}
