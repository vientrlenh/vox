package com.sep.vox.interfaces.rest.controller.internal;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.query.GetTurnUploadUrlQuery;
import com.sep.vox.application.port.input.usecase.assessmentturn.GetTurnUploadUrlUseCase;
import com.sep.vox.application.response.input.assessmentturn.TurnUploadUrlResponse;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

/**
 * Issues the upload URL for ASSESSMENT turn audio to Python, parallel to
 * {@code /internal/practice-sessions/{id}/turns/{turnOrder}/upload-url} on the practice side.
 *
 * <p>Why an internal version is needed when {@code /api/v1/assessment-turns/upload-url} already exists: that route carries
 * {@code @PreAuthorize("hasRole('STUDENT')")} so only the student's machine can call it. From now on the audio of an
 * assessment turn is uploaded by Python and not by WPF -- exactly the model chosen for practice, where
 * {@code practice_session_client} states why the old model was rejected: it depends on the student's
 * own network path, while the pod reaches S3 inside the same AWS.
 *
 * <p>Both routes SHARE {@link GetTurnUploadUrlUseCase}, so the S3 key and the TTL cannot diverge
 * between the two paths.
 *
 * <p>Protected by {@code X-Internal-Secret}
 * ({@code com.sep.vox.infrastructure.filter.PracticeInternalSecretFilter}), not by a JWT.
 */
@RestController
@RequestMapping("/internal/assessment-turns")
@RequiredArgsConstructor
public class AssessmentTurnInternalController {

    private final GetTurnUploadUrlUseCase getTurnUploadUrlUseCase;

    @GetMapping("/{attemptAnswerId}/turns/{turnOrder}/upload-url")
    public ApiResponse<TurnUploadUrlResponse> turnUploadUrl(
            @PathVariable UUID attemptAnswerId,
            @PathVariable int turnOrder) {
        return ApiResponse.success(
            "OK",
            getTurnUploadUrlUseCase.execute(new GetTurnUploadUrlQuery(attemptAnswerId, turnOrder))
        );
    }
}
