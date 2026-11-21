package com.sep.vox.domain.model.personalization;

/** An error the client reports together with a speaking turn, before it is graded/confirmed. */

public class TurnCorrectionSubmission {

    private String category;
    private String originalText;
    private String correctedText;
    private String explanation;
    private String correctAudioUrl;
    private double confidence;

    public TurnCorrectionSubmission() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getCorrectedText() {
        return correctedText;
    }

    public void setCorrectedText(String correctedText) {
        this.correctedText = correctedText;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getCorrectAudioUrl() {
        return correctAudioUrl;
    }

    public void setCorrectAudioUrl(String correctAudioUrl) {
        this.correctAudioUrl = correctAudioUrl;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final TurnCorrectionSubmission target = new TurnCorrectionSubmission();

        private Builder() {
        }

        public Builder category(String category) {
            target.category = category;
            return this;
        }

        public Builder originalText(String originalText) {
            target.originalText = originalText;
            return this;
        }

        public Builder correctedText(String correctedText) {
            target.correctedText = correctedText;
            return this;
        }

        public Builder explanation(String explanation) {
            target.explanation = explanation;
            return this;
        }

        public Builder correctAudioUrl(String correctAudioUrl) {
            target.correctAudioUrl = correctAudioUrl;
            return this;
        }

        public Builder confidence(double confidence) {
            target.confidence = confidence;
            return this;
        }

        public TurnCorrectionSubmission build() {
            return target;
        }

    }
}
