package com.sep.vox.application.response.input.recording;

import com.sep.vox.domain.model.assessment.AssessmentRecordingAssemblyStatus;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import java.util.UUID;

/**
 * One assessment recording together with a playback link, for the grading screen.
 *
 * <p>Deliberately does NOT carry {@code bucket} and {@code s3Key}: they are storage details, and once exposed
 * clients would build URLs themselves -- exactly what makes links lose their expiry and lose permission control.
 */
public record AssessmentRecordingPlaybackResponse(
    UUID id,
    AssessmentRequiredStreamType streamType,
    AssessmentRecordingAssemblyStatus status,
    String source,
    Integer durationSeconds,
    boolean canonical,
    String playbackUrl
) {
}
