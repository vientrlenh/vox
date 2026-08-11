package com.sep.vox.domain.dto.personalization;

import java.util.List;
import java.util.UUID;

public record InterestQuizItemDto(
    UUID id,
    List<String> statements
) {
}
