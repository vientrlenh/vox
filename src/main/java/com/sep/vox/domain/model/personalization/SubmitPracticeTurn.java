package com.sep.vox.domain.model.personalization;

import java.util.List;
import java.util.UUID;

public class SubmitPracticeTurn {

    private UUID sessionId;
    private UUID questionId;
    private int turnOrder;
    private String turnType;
    private String promptText;
    private String audioUrl;
    private String transcript;
    private int durationSeconds;
    private String wordFeedbackJson;
    private Double turnScore;
    private boolean questionComplete;
    private List<TurnCorrectionSubmission> corrections;

    public SubmitPracticeTurn() {
    }

    public SubmitPracticeTurn(
            UUID sessionId,
            UUID questionId,
            int turnOrder,
            String turnType,
            String promptText,
            String audioUrl,
            String transcript,
            int durationSeconds,
            String wordFeedbackJson,
            Double turnScore,
            boolean questionComplete,
            List<TurnCorrectionSubmission> corrections) {
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.turnOrder = turnOrder;
        this.turnType = turnType;
        this.promptText = promptText;
        this.audioUrl = audioUrl;
        this.transcript = transcript;
        this.durationSeconds = durationSeconds;
        this.wordFeedbackJson = wordFeedbackJson;
        this.turnScore = turnScore;
        this.questionComplete = questionComplete;
        this.corrections = corrections;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public int getTurnOrder() {
        return turnOrder;
    }

    public void setTurnOrder(int turnOrder) {
        this.turnOrder = turnOrder;
    }

    public String getTurnType() {
        return turnType;
    }

    public void setTurnType(String turnType) {
        this.turnType = turnType;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getWordFeedbackJson() {
        return wordFeedbackJson;
    }

    public void setWordFeedbackJson(String wordFeedbackJson) {
        this.wordFeedbackJson = wordFeedbackJson;
    }

    public Double getTurnScore() {
        return turnScore;
    }

    public void setTurnScore(Double turnScore) {
        this.turnScore = turnScore;
    }

    public boolean isQuestionComplete() {
        return questionComplete;
    }

    public void setQuestionComplete(boolean questionComplete) {
        this.questionComplete = questionComplete;
    }

    public List<TurnCorrectionSubmission> getCorrections() {
        return corrections;
    }

    public void setCorrections(List<TurnCorrectionSubmission> corrections) {
        this.corrections = corrections;
    }
}
