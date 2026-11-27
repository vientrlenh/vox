package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.assessmentappeal.ApproveAssessmentAppealCommand;
import com.sep.vox.application.port.input.command.assessmentappeal.AssignAssessmentAppealReviewerCommand;
import com.sep.vox.application.port.input.command.assessmentappeal.CreateAssessmentAppealCommand;
import com.sep.vox.application.port.input.command.assessmentappeal.RejectAssessmentAppealCommand;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.assessmentappeal.ApproveAndClaimAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ApproveAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.AssignAssessmentAppealReviewerUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.CreateAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.RejectAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.WithdrawAssessmentAppealUseCase;
import com.sep.vox.interfaces.rest.dto.request.assessmentappeal.ApproveAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentappeal.AssignAssessmentAppealReviewerRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentappeal.CreateAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentappeal.RejectAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

/**
 * The admin now only COORDINATES appeals: approve / reject / assign a grader.
 *
 * <p>Two old endpoints were removed: {@code /reviewers/me/report} and {@code /publish}.
 * Appeal graders now share the grading screen with the other three rounds
 * ({@code /api/v1/grading-assignments/{id}/uphold|regrade}), and submitting publishes the
 * result right away -- there is no longer an admin step that cross-checks and presses publish.
 */
@RestController
@RequestMapping("/api/v1/assessment-appeals")
@RequiredArgsConstructor
public class RestAssessmentAppealController {

    private final CreateAssessmentAppealUseCase createAssessmentAppealUseCase;
    private final ApproveAssessmentAppealUseCase approveAssessmentAppealUseCase;
    private final ApproveAndClaimAssessmentAppealUseCase approveAndClaimAssessmentAppealUseCase;
    private final RejectAssessmentAppealUseCase rejectAssessmentAppealUseCase;
    private final AssignAssessmentAppealReviewerUseCase assignAssessmentAppealReviewerUseCase;
    private final WithdrawAssessmentAppealUseCase withdrawAssessmentAppealUseCase;

    @Operation(summary = "Student files an appeal for one or more sections of an assessment")
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<UUID>> createAppeal(
            @Valid @RequestBody CreateAssessmentAppealRequest request) {
        CreateAssessmentAppealCommand command = CreateAssessmentAppealRequest.toCommand(request);
        UUID appealId = createAssessmentAppealUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Appeal submitted successfully!", appealId));
    }

    @Operation(summary = "Student withdraws an appeal while it is still pending approval. The appeal attempt is refunded.")
    @PostMapping("/{appealId}/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<UUID>> withdrawAppeal(@PathVariable("appealId") UUID appealId) {
        return ResponseEntity.ok(
            ApiResponse.success("Appeal withdrawn successfully!", withdrawAssessmentAppealUseCase.execute(appealId)));
    }

    @Operation(summary = "Approves an appeal and sets its processing deadline")
    @PostMapping("/{appealId}/approve")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> approveAppeal(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody ApproveAssessmentAppealRequest request) {
        ApproveAssessmentAppealCommand command = ApproveAssessmentAppealRequest.toCommand(appealId, request);
        UUID responseId = approveAssessmentAppealUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Appeal approved successfully!", responseId));
    }

    @Operation(summary = "The teacher in charge of the class test approves the appeal and claims the grading in one step. "
        + "The default processing deadline is T+3 at 17:00. Returns the id of the assignment row that was just opened.")
    @PostMapping("/{appealId}/approve-and-claim")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> approveAndClaimAppeal(@PathVariable("appealId") UUID appealId) {
        return ResponseEntity.ok(ApiResponse.success(
            "Appeal approved and grading claimed!", approveAndClaimAssessmentAppealUseCase.execute(appealId)));
    }

    @Operation(summary = "Rejects an appeal (a reason is required)")
    @PostMapping("/{appealId}/reject")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> rejectAppeal(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody RejectAssessmentAppealRequest request) {
        RejectAssessmentAppealCommand command = RejectAssessmentAppealRequest.toCommand(appealId, request);
        UUID responseId = rejectAssessmentAppealUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Appeal rejected successfully!", responseId));
    }

    @Operation(summary = "Assigns ONE teacher to grade the appeal. Anyone who already graded this result is refused, "
        + "unless `overrideReason` is passed -- that reason is recorded on the appeal.")
    @PostMapping("/{appealId}/reviewer")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> assignReviewer(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody AssignAssessmentAppealReviewerRequest request) {
        AssignAssessmentAppealReviewerCommand command = AssignAssessmentAppealReviewerRequest.toCommand(appealId, request);
        UUID assignmentId = assignAssessmentAppealReviewerUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Appeal grader assigned successfully!", assignmentId));
    }
}
