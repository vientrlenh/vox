package com.sep.vox.interfaces.graphql.dto.request;

import java.util.UUID;

public record InterestQuizAnswerInput(
    UUID itemId,
    int mostStatementIndex,
    int leastStatementIndex
) {
}
