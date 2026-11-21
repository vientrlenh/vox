package com.sep.vox.domain.model.personalization;

import java.util.List;
import java.util.UUID;

public class InterestQuizSeedItem {

    private UUID id;
    private List<String> dimensionPerStatement;
    private List<String> statements;
    private String note;

    public InterestQuizSeedItem() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public List<String> getDimensionPerStatement() {
        return dimensionPerStatement;
    }

    public void setDimensionPerStatement(List<String> dimensionPerStatement) {
        this.dimensionPerStatement = dimensionPerStatement;
    }

    public List<String> getStatements() {
        return statements;
    }

    public void setStatements(List<String> statements) {
        this.statements = statements;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final InterestQuizSeedItem target = new InterestQuizSeedItem();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder dimensionPerStatement(List<String> dimensionPerStatement) {
            target.dimensionPerStatement = dimensionPerStatement;
            return this;
        }

        public Builder statements(List<String> statements) {
            target.statements = statements;
            return this;
        }

        public Builder note(String note) {
            target.note = note;
            return this;
        }

        public InterestQuizSeedItem build() {
            return target;
        }

    }
}
