package com.sep.vox.interfaces.graphql.input.learnerprofile;

import java.util.UUID;

public record InterestQuizAnswerInput(
    UUID itemId,
    int mostStatementIndex,
    int leastStatementIndex
) {
}
