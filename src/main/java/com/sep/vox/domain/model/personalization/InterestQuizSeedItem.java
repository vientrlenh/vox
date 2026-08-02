package com.sep.vox.domain.model.personalization;

import java.util.List;
import java.util.UUID;

public record InterestQuizSeedItem(
    UUID id,
    List<String> dimensionPerStatement,
    List<String> statements,
    String note
) {
}
