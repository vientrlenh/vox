package com.sep.vox.interfaces.graphql.dto.request.learnerprofile;

import java.util.UUID;

public record InterestQuizAnswerInput(
    UUID itemId,
    int mostStatementIndex,
    int leastStatementIndex
) {
}
