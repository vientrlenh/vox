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

    public InterestQuizSeedItem(
            UUID id,
            List<String> dimensionPerStatement,
            List<String> statements,
            String note) {
        this.id = id;
        this.dimensionPerStatement = dimensionPerStatement;
        this.statements = statements;
        this.note = note;
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
}
