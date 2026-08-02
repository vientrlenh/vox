package com.sep.vox.domain.model.personalization;

import java.util.UUID;

/** Một câu trả lời "giống nhất / ít giống nhất" trong quiz sở thích 5-7 bộ triplet. */

public class QuizAnswer {

    private UUID itemId;
    private int mostStatementIndex;
    private int leastStatementIndex;

    public QuizAnswer() {
    }

    public QuizAnswer(
            UUID itemId,
            int mostStatementIndex,
            int leastStatementIndex) {
        this.itemId = itemId;
        this.mostStatementIndex = mostStatementIndex;
        this.leastStatementIndex = leastStatementIndex;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public int getMostStatementIndex() {
        return mostStatementIndex;
    }

    public void setMostStatementIndex(int mostStatementIndex) {
        this.mostStatementIndex = mostStatementIndex;
    }

    public int getLeastStatementIndex() {
        return leastStatementIndex;
    }

    public void setLeastStatementIndex(int leastStatementIndex) {
        this.leastStatementIndex = leastStatementIndex;
    }
}
