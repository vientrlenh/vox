package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.response.input.assessment.StudentAssessmentPaperResponse;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import java.util.UUID;

import com.sep.vox.application.port.input.command.assessmentsession.CompleteAssessmentSessionGradingCommand;
import com.sep.vox.application.port.input.command.assessmentsession.CreateAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.assessmentsession.DeleteAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.assessmentsession.FlagAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.assessmentsession.ForceEndAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.assessmentsession.HandOffGradingToHumanCommand;
import com.sep.vox.application.port.input.command.assessmentsession.RetryGradingAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.assessmentsession.UpdateAssessmentSessionRemainingTimeCommand;
import com.sep.vox.application.port.input.command.assessmentsession.UpdateAssessmentSessionStatusCommand;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.query.assessment.ViewAssessmentSessionPaperQuery;
import com.sep.vox.application.port.input.usecase.assessment.GetAssessmentSessionPaperUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.CompleteAssessmentSessionGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.CreateAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.DeleteAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.FlagAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.ForceEndAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.ReportAiUsageUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.HandOffGradingToHumanUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.RetryGradingAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionRemainingTimeUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionStatusUseCase;
import com.sep.vox.interfaces.rest.dto.request.assessmentsession.CreateAssessmentSessionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentsession.ReportAiUsageRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentsession.SessionReasonRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentsession.UpdateAssessmentSessionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentsession.UpdateRemainingTimeRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
@RequiredArgsConstructor
public class RestAssessmentSessionController {

    private final CreateAssessmentSessionUseCase createAssessmentSessionUseCase;
    private final CompleteAssessmentSessionGradingUseCase completeAssessmentSessionGradingUseCase;
    private final UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase;
    private final GetAssessmentSessionPaperUseCase getAssessmentSessionPaperUseCase;
    private final DeleteAssessmentSessionUseCase deleteAssessmentSessionUseCase;
    private final FlagAssessmentSessionUseCase flagAssessmentSessionUseCase;
    private final ForceEndAssessmentSessionUseCase forceEndAssessmentSessionUseCase;
    private final RetryGradingAssessmentSessionUseCase retryGradingAssessmentSessionUseCase;
    private final HandOffGradingToHumanUseCase handOffGradingToHumanUseCase;
    private final UpdateAssessmentSessionRemainingTimeUseCase updateAssessmentSessionRemainingTimeUseCase;
    private final ReportAiUsageUseCase reportAiUsageUseCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CreateAssessmentSessionRequest request) {
        AssessmentSessionResponse data = createAssessmentSessionUseCase.execute(new CreateAssessmentSessionCommand(
                request.assessmentId(),
                request.candidateId(),
                request.paperId()
        ));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Session created successfully", data));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentSessionRequest request) {
        AssessmentSessionResponse data = updateAssessmentSessionStatusUseCase.execute(new UpdateAssessmentSessionStatusCommand(
                id,
                request.status()
        ));
        return ResponseEntity.ok(ApiResponse.success("Session status updated successfully", data));
    }

    /**
     * Records the client's countdown timer (every 10 seconds by default) so that re-entering the session
     * does not start from the beginning. Returns the value the server actually keeps, which can be lower than the
     * value sent if it is capped from above or rejected because it does not go backwards.
     */
    @PatchMapping("/{id}/remaining-time")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Integer>> updateRemainingTime(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateRemainingTimeRequest request) {
        Integer data = updateAssessmentSessionRemainingTimeUseCase.execute(
            new UpdateAssessmentSessionRemainingTimeCommand(id, request.remainingSeconds()));
        return ResponseEntity.ok(ApiResponse.success("Remaining time updated successfully", data));
    }

    @GetMapping("/{id}/paper")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getPaper(@PathVariable("id") UUID id) {
        StudentAssessmentPaperResponse data = getAssessmentSessionPaperUseCase.execute(new ViewAssessmentSessionPaperQuery(id));
        return ResponseEntity.ok(ApiResponse.success("Paper retrieved successfully", data));
    }

    @PostMapping("/{id}/flag")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> flag(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SessionReasonRequest request) {
        UUID data = flagAssessmentSessionUseCase.execute(new FlagAssessmentSessionCommand(id, true, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Session flagged as suspicious successfully", data));
    }

    @PostMapping("/{id}/force-end")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> forceEnd(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SessionReasonRequest request) {
        UUID data = forceEndAssessmentSessionUseCase.execute(new ForceEndAssessmentSessionCommand(id, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Session ended by force successfully", data));
    }

    @PostMapping("/{id}/retry-grading")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> retryGrading(@PathVariable("id") UUID id) {
        UUID data = retryGradingAssessmentSessionUseCase.execute(new RetryGradingAssessmentSessionCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Re-grade request submitted successfully", data));
    }

    /**
     * The AI failed to grade the result -> put it in the queue for human graders. Returns the id of the result
     * just created, so the client knows the result is now in the queue.
     */
    @PostMapping("/{id}/hand-off-grading")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> handOffGrading(@PathVariable("id") UUID id) {
        UUID data = handOffGradingToHumanUseCase.execute(new HandOffGradingToHumanCommand(id));
        return ResponseEntity.ok(ApiResponse.success(
            "Moved to manual grading, waiting for the school to assign a teacher", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<?>> delete(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody SessionReasonRequest request) {
        deleteAssessmentSessionUseCase.execute(new DeleteAssessmentSessionCommand(id, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Session deleted successfully", null));
    }

     @PostMapping("/{sessionId}/complete-grading")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> completeGrading(@PathVariable(name = "sessionId") UUID sessionId) {
        completeAssessmentSessionGradingUseCase.execute(new CompleteAssessmentSessionGradingCommand(sessionId));
        return ResponseEntity.ok(ApiResponse.success("Grading completion recorded successfully"));
    }

    /**
     * A REST path parallel to the Kafka topic ai-usage-recorded -- for sources with no direct
     * Kafka connection, for example the desktop app that aggregates TTS itself on the learner's machine
     * (the desktop app, LocalAvatarSpeaker.cs) instead of going through the Agentic AI. See ReportAiUsageUseCase.
     */
    @PostMapping("/{id}/ai-usage")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> reportAiUsage(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ReportAiUsageRequest request) {
        reportAiUsageUseCase.execute(ReportAiUsageRequest.toCommand(id, request));
        return ResponseEntity.ok(ApiResponse.success("AI cost recorded successfully"));
    }
}
