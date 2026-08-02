package com.sep.vox.domain.dto.personalization;

import java.util.List;

public record TopicSearchResultDto(
    List<PracticeTopicOfferDto> topics,
    boolean canGenerate
) {
}
