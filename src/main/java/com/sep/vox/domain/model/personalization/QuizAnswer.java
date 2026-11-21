package com.sep.vox.domain.model.personalization;

import java.util.UUID;

/** One "most alike / least alike" answer in the interest quiz of 5-7 triplets. */

public class QuizAnswer {

    private UUID itemId;
    private int mostStatementIndex;
    private int leastStatementIndex;

    public QuizAnswer() {
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final QuizAnswer target = new QuizAnswer();

        private Builder() {
        }

        public Builder itemId(UUID itemId) {
            target.itemId = itemId;
            return this;
        }

        public Builder mostStatementIndex(int mostStatementIndex) {
            target.mostStatementIndex = mostStatementIndex;
            return this;
        }

        public Builder leastStatementIndex(int leastStatementIndex) {
            target.leastStatementIndex = leastStatementIndex;
            return this;
        }

        public QuizAnswer build() {
            return target;
        }

    }
}
