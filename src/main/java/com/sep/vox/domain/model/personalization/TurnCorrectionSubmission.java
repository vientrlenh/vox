package com.sep.vox.domain.model.personalization;

/** Một lỗi được client báo kèm khi nộp lượt nói, trước khi được chấm/xác nhận. */

public class TurnCorrectionSubmission {

    private String category;
    private String originalText;
    private String correctedText;
    private String explanation;
    private String correctAudioUrl;
    private double confidence;

    public TurnCorrectionSubmission() {
    }

    public TurnCorrectionSubmission(
            String category,
            String originalText,
            String correctedText,
            String explanation,
            String correctAudioUrl,
            double confidence) {
        this.category = category;
        this.originalText = originalText;
        this.correctedText = correctedText;
        this.explanation = explanation;
        this.correctAudioUrl = correctAudioUrl;
        this.confidence = confidence;
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
}
