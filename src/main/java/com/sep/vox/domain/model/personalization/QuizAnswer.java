package com.sep.vox.domain.model.personalization;

import java.util.UUID;

/** Một câu trả lời "giống nhất / ít giống nhất" trong quiz sở thích 5-7 bộ triplet. */
public record QuizAnswer(
    UUID itemId,
    int mostStatementIndex,
    int leastStatementIndex
) {
}
