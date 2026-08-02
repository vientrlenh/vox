package com.sep.vox.domain.model.personalization;

public class PracticeCriterionScoreEntry {

    private String criterionCode;
    private double score;
    private String matchedBandCode;

    public PracticeCriterionScoreEntry() {
    }

    public PracticeCriterionScoreEntry(
            String criterionCode,
            double score,
            String matchedBandCode) {
        this.criterionCode = criterionCode;
        this.score = score;
        this.matchedBandCode = matchedBandCode;
    }

    public String getCriterionCode() {
        return criterionCode;
    }

    public void setCriterionCode(String criterionCode) {
        this.criterionCode = criterionCode;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getMatchedBandCode() {
        return matchedBandCode;
    }

    public void setMatchedBandCode(String matchedBandCode) {
        this.matchedBandCode = matchedBandCode;
    }
}
