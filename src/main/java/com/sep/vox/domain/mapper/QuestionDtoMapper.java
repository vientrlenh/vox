package com.sep.vox.domain.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.model.languagelevel.LevelFramework;
import com.sep.vox.domain.model.question.Question;

public class QuestionDtoMapper {

    public static QuestionDto toDto(Question domain, String standardLevelCode,
            String frameworkCode, String frameworkName) {
        return new QuestionDto(
            domain.getId(),
            domain.getTopicId(),
            domain.getQuestionText(),
            domain.getAudioUrl(),
            domain.getStandardLevelId(),
            standardLevelCode,
            frameworkCode,
            frameworkName,
            domain.getQuestionType().value(),
            domain.getDurationSeconds(),
            domain.isActive(),
            domain.getCreatedAt() != null ? domain.getCreatedAt().toString() : null
        );
    }

    public static List<QuestionDto> toDtoList(List<Question> list,
            Map<UUID, String> standardLevelCodeMap,
            Map<UUID, UUID> standardLevelToFrameworkMap,
            Map<UUID, LevelFramework> frameworkMap) {
        return list.stream()
            .map(q -> {
                var frameworkId = standardLevelToFrameworkMap.get(q.getStandardLevelId());
                var framework = frameworkId != null ? frameworkMap.get(frameworkId) : null;
                return toDto(
                    q,
                    standardLevelCodeMap.get(q.getStandardLevelId()),
                    framework != null ? framework.getCode().value() : null,
                    framework != null ? framework.getName() : null
                );
            })
            .toList();
    }

    public static PageResult<QuestionDto> toDtoPage(PageResult<Question> page,
            Map<UUID, String> standardLevelCodeMap,
            Map<UUID, UUID> standardLevelToFrameworkMap,
            Map<UUID, LevelFramework> frameworkMap) {
        return new PageResult<>(
            toDtoList(page.content(), standardLevelCodeMap, standardLevelToFrameworkMap, frameworkMap),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }
}
