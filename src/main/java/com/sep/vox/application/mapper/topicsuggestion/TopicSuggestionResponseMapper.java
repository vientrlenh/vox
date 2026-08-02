package com.sep.vox.application.mapper.topicsuggestion;

import java.util.List;

import com.sep.vox.application.mapper.practiceplanning.PracticePlanningResponseMapper;
import com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicFromKeywordResult;
import com.sep.vox.application.response.input.topicsuggestion.TopicSuggestionResponses.TopicSuggestion;
import com.sep.vox.domain.dto.personalization.TopicFromKeywordResultDto;
import com.sep.vox.domain.dto.personalization.TopicSuggestionDto;

public final class TopicSuggestionResponseMapper {

    private TopicSuggestionResponseMapper() {
    }

    public static TopicSuggestion toResponse(TopicSuggestionDto dto) {
        return new TopicSuggestion(
            dto.id(),
            dto.suggestedTopicName(),
            dto.interestDimension(),
            dto.confidence(),
            dto.reasonText(),
            dto.status()
        );
    }

    public static List<TopicSuggestion> toResponseList(List<TopicSuggestionDto> dtos) {
        return dtos.stream().map(TopicSuggestionResponseMapper::toResponse).toList();
    }

    public static TopicFromKeywordResult toResponse(TopicFromKeywordResultDto dto) {
        return new TopicFromKeywordResult(
            PracticePlanningResponseMapper.toResponse(dto.topic()),
            dto.outcome()
        );
    }
}
