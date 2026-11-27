package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.QuestionAssetType;

/**
 * Checks whether an asset has enough content for the AI to work with.
 *
 * <p><b>Why it is required</b>: Python does NOT see the image or hear the audio at all --
 * {@code QuestionAssetContext} only carries {@code {type, transcript, description, alt_text}}, all
 * text. And the branch where the AI analyses the asset itself is switched off (auto-publish was dropped in both Create and
 * Update), so this text is the AI's <b>entire</b> knowledge of the resource.
 *
 * <p>If left blank, every question turn gets exactly the string {@code "Asset details: unavailable"} in the prompt --
 * the follow-up is generic and cannot hold on to any detail, and {@code ValidityNode} grades on-topic
 * without knowing what the question is about. A silent failure: no error, no log, just a wrong score.
 *
 * <p>Kept in one place instead of copied into both Create and Update: two copies would drift apart, and drifting here
 * means an asset can be edited into a state that creation would reject.
 */
public final class QuestionAssetContentValidator {

    private QuestionAssetContentValidator() {
    }

    public static void validate(
            QuestionAssetType type,
            String url,
            String transcript,
            String description,
            String altText) {
        if (type == QuestionAssetType.TEXT_PASSAGE) {
            requireTextPassageContent(transcript);
            return;
        }

        if (isBlank(url)) {
            throw new IllegalArgumentException("The resource URL must not be empty");
        }

        if (type == QuestionAssetType.IMAGE) {
            requireImageDescription(description, altText);
            return;
        }

        requireMediaTranscript(transcript);
    }

    private static void requireTextPassageContent(String transcript) {
        if (isBlank(transcript)) {
            throw new IllegalArgumentException("The passage content must not be empty");
        }
    }

    private static void requireImageDescription(String description, String altText) {
        if (isBlank(description) && isBlank(altText)) {
            throw new IllegalArgumentException(
                "An image needs a content description (or alt text). The AI cannot see the image — "
                    + "it only knows the image through this description, and if it is left empty the AI will ask and grade in generic terms.");
        }
    }

    private static void requireMediaTranscript(String transcript) {
        if (isBlank(transcript)) {
            throw new IllegalArgumentException(
                "Audio/video needs a transcript. The AI cannot listen to the file — "
                    + "it only knows the content through this transcript, and if it is left empty the AI will ask and grade in generic terms.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
