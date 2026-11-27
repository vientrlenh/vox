package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.assessmentgrading.AssignGradingCommand;
import com.sep.vox.application.port.input.command.assessmentgrading.AutoAssignGradingCommand;
import com.sep.vox.application.port.input.command.assessmentgrading.GradingDecisionCommand;
import com.sep.vox.application.port.input.command.assessmentgrading.ReassignGradingCommand;
import com.sep.vox.application.port.input.command.assessmentgrading.ReclaimOverdueAssignmentsCommand;
import com.sep.vox.application.port.input.command.assessmentgrading.RemoveGradingAssignmentCommand;
import com.sep.vox.application.port.input.command.assessmentgrading.SetGradingDeadlineCommand;
import com.sep.vox.application.port.input.command.assessmentgrading.SubmitGradingCommand;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.assessmentgrading.AssignGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.AutoAssignGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ClearInvalidResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.DeclineGradingAssignmentUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.InvalidateResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.PreviewGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ReassignGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.RegradeResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ReclaimOverdueAssignmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.RemoveGradingAssignmentUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.SetGradingDeadlineUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.UpholdResultUseCase;
import com.sep.vox.application.response.input.assessmentgrading.GradingActionResponse;
import com.sep.vox.application.response.input.assessmentgrading.GradingPreviewResponse;
import com.sep.vox.application.response.input.assessmentgrading.ReclaimOverdueResponse;
import com.sep.vox.interfaces.rest.dto.request.assessmentgrading.AssignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentgrading.AutoAssignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentgrading.GradingDecisionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentgrading.ReclaimOverdueAssignmentsRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentgrading.SetGradingDeadlineRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentgrading.ReassignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentgrading.SubmitGradingRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

/**
 * The four teacher actions live side by side here -- {@code /uphold}, {@code /regrade},
 * {@code /invalidate}, {@code /clear-invalid} -- shared by all four grading rounds. Which round
 * allows which action is decided by the backend ({@code GradingRoundPolicy}) and is returned
 * ready-made in {@code gradingTaskDetail.allowedOutcomes}.
 */
@RestController
@RequestMapping("/api/v1/grading-assignments")
@RequiredArgsConstructor
public class RestGradingAssignmentController {

    private final AssignGradingUseCase assignGradingUseCase;
    private final AutoAssignGradingUseCase autoAssignGradingUseCase;
    private final ReassignGradingUseCase reassignGradingUseCase;
    private final RemoveGradingAssignmentUseCase removeGradingAssignmentUseCase;
    private final PreviewGradingUseCase previewGradingUseCase;
    private final UpholdResultUseCase upholdResultUseCase;
    private final RegradeResultUseCase regradeResultUseCase;
    private final InvalidateResultUseCase invalidateResultUseCase;
    private final ClearInvalidResultUseCase clearInvalidResultUseCase;
    private final DeclineGradingAssignmentUseCase declineGradingAssignmentUseCase;
    private final SetGradingDeadlineUseCase setGradingDeadlineUseCase;
    private final ReclaimOverdueAssignmentsUseCase reclaimOverdueAssignmentsUseCase;

    @Operation(summary = "Assigns teachers to grade (manual assignment, several results at once, one grading round)")
    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> assign(
            @Valid @RequestBody AssignGradingRequest request) {
        AssignGradingCommand command = AssignGradingRequest.toCommand(request);
        List<UUID> assignmentIds = assignGradingUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Grading assigned successfully!", assignmentIds));
    }

    @Operation(summary = "Automatic assignment. Picks results in 4 modes (all / random N% / "
        + "high risk / specified list) and then spreads them evenly by the current load of the teacher group.")
    @PostMapping("/auto")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> autoAssign(
            @Valid @RequestBody AutoAssignGradingRequest request) {
        AutoAssignGradingCommand command = AutoAssignGradingRequest.toCommand(request);
        List<UUID> assignmentIds = autoAssignGradingUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Automatic assignment successful!", assignmentIds));
    }

    @Operation(summary = "Changes the teacher grading a result (only while grading is not finished)")
    @PutMapping("/{assignmentId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> reassign(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody ReassignGradingRequest request) {
        ReassignGradingCommand command = ReassignGradingRequest.toCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Grader changed successfully!", reassignGradingUseCase.execute(command)));
    }

    @Operation(summary = "Removes a grading assignment (only while grading is not finished)")
    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> remove(@PathVariable("assignmentId") UUID assignmentId) {
        RemoveGradingAssignmentCommand command = new RemoveGradingAssignmentCommand(assignmentId);
        return ResponseEntity.ok(
            ApiResponse.success("Assignment removed successfully!", removeGradingAssignmentUseCase.execute(command)));
    }

    @Operation(summary = "Sets / edits the grading deadline of one or more assignments. "
        + "Leave `deadlineAt` empty to remove the deadline.")
    @PutMapping("/deadline")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> setDeadline(
            @Valid @RequestBody SetGradingDeadlineRequest request) {
        SetGradingDeadlineCommand command = SetGradingDeadlineRequest.toCommand(request);
        return ResponseEntity.ok(
            ApiResponse.success("Grading deadline set successfully!", setGradingDeadlineUseCase.execute(command)));
    }

    @Operation(summary = "Reclaims overdue assignments and reassigns them right away if a replacement teacher group is chosen. "
        + "Leave `assignmentIds` empty to reclaim every overdue assignment of the assessment. "
        + "Returns two separate lists: `reclaimedAssignmentIds` (old assignments just closed) and "
        + "`reassignedAssignmentIds` (new assignments just opened, empty if only reclaimed). "
        + "Each run handles at most 500 assignments; `hasMore = true` means overdue rows "
        + "remain unprocessed, so call again to run the next batch.")
    @PostMapping("/reclaim-overdue")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<ReclaimOverdueResponse>> reclaimOverdue(
            @Valid @RequestBody ReclaimOverdueAssignmentsRequest request) {
        ReclaimOverdueAssignmentsCommand command = ReclaimOverdueAssignmentsRequest.toCommand(request);
        return ResponseEntity.ok(
            ApiResponse.success("Overdue assignments reclaimed successfully!",
                reclaimOverdueAssignmentsUseCase.execute(command)));
    }

    @Operation(summary = "Uphold the score -- the teacher confirms the current score is correct. "
        + "In the first grading round the result is published right away (no further admin review needed).")
    @PostMapping("/{assignmentId}/uphold")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingActionResponse>> uphold(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody(required = false) GradingDecisionRequest request) {
        GradingDecisionCommand command = GradingDecisionRequest.toCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Result score upheld!", upholdResultUseCase.execute(command)));
    }

    @Operation(summary = "Re-grade -- submitting is final. Scores are entered per criterion of each section; "
        + "the total and the rating are recomputed by the system. A result flagged as suspicious has its flag cleared on submit.")
    @PostMapping("/{assignmentId}/regrade")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingActionResponse>> regrade(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody SubmitGradingRequest request) {
        SubmitGradingCommand command = SubmitGradingRequest.toCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Grading scores submitted successfully!", regradeResultUseCase.execute(command)));
    }

    @Operation(summary = "Dry-run of the total score for the scores being entered. Writes NOTHING. "
        + "It is a POST because the body is identical to /regrade -- the returned total equals the total on submit.")
    @PostMapping("/{assignmentId}/regrade/preview")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingPreviewResponse>> previewRegrade(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody SubmitGradingRequest request) {
        SubmitGradingCommand command = SubmitGradingRequest.toCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Score preview calculated successfully!", previewGradingUseCase.execute(command)));
    }

    @Operation(summary = "Concludes the result is a violation -> invalidates the result, no scores entered. A reason is required.")
    @PostMapping("/{assignmentId}/invalidate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingActionResponse>> invalidate(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody GradingDecisionRequest request) {
        GradingDecisionCommand command = GradingDecisionRequest.toCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Result invalidated successfully!", invalidateResultUseCase.execute(command)));
    }

    @Operation(summary = "Concludes there is NO violation -> clears the invalidation, unblocks the candidate, and immediately opens "
        + "the first grading round for this same teacher. A reason is required.")
    @PostMapping("/{assignmentId}/clear-invalid")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingActionResponse>> clearInvalid(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody GradingDecisionRequest request) {
        GradingDecisionCommand command = GradingDecisionRequest.toCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Result invalidation cleared!", clearInvalidResultUseCase.execute(command)));
    }

    @Operation(summary = "Returns the assignment with a reason (knows the candidate...). "
        + "The result goes back to the unassigned pool and the admin is notified.")
    @PostMapping("/{assignmentId}/decline")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingActionResponse>> decline(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody GradingDecisionRequest request) {
        GradingDecisionCommand command = GradingDecisionRequest.toCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Assignment declined!", declineGradingAssignmentUseCase.execute(command)));
    }

}
