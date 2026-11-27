package com.sep.vox.interfaces.rest.controller;

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

import com.sep.vox.application.port.input.command.assessmentcandidate.BulkDeleteAssessmentCandidatesCommand;
import com.sep.vox.application.port.input.command.assessmentcandidate.DeleteAssessmentCandidateCommand;
import com.sep.vox.application.port.input.command.assessmentcandidate.UnblockAssessmentCandidateCommand;
import com.sep.vox.application.port.input.command.assessmentcandidate.UpdateAssessmentCandidateStatusCommand;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.AddAssessmentCandidateUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.AssignAssessmentCandidateScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.AssignAssessmentPapersUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.AutoFillAssessmentCandidatesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.BulkAssignAssessmentCandidateScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.BulkDeleteAssessmentCandidatesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.DeleteAssessmentCandidateUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ImportAssessmentCandidatesFromClassUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ImportAssessmentCandidatesFromGradeUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UnblockAssessmentCandidateUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UpdateAssessmentCandidateStatusUseCase;
import com.sep.vox.application.response.input.assessmentcandidate.AssignAssessmentPapersResponse;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.interfaces.rest.dto.request.assessmentcandidate.AddAssessmentCandidateRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentcandidate.AssignAssessmentCandidateScheduleRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentcandidate.AssignAssessmentPapersRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentcandidate.AutoFillAssessmentCandidatesRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentcandidate.BulkAssignAssessmentCandidateScheduleRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentcandidate.BulkDeleteAssessmentCandidatesRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentcandidate.ImportAssessmentCandidatesFromClassRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentcandidate.ImportAssessmentCandidatesFromGradeRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentsession.SessionReasonRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentcandidate.UpdateAssessmentCandidateStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/assessments/{assessmentId}/candidates")
@RequiredArgsConstructor
public class RestAssessmentCandidateController {

    private final AddAssessmentCandidateUseCase addAssessmentCandidateUseCase;
    private final ImportAssessmentCandidatesFromClassUseCase importAssessmentCandidatesFromClassUseCase;
    private final ImportAssessmentCandidatesFromGradeUseCase importAssessmentCandidatesFromGradeUseCase;
    private final AssignAssessmentCandidateScheduleUseCase assignAssessmentCandidateScheduleUseCase;
    private final BulkAssignAssessmentCandidateScheduleUseCase bulkAssignAssessmentCandidateScheduleUseCase;
    private final AutoFillAssessmentCandidatesUseCase autoFillAssessmentCandidatesUseCase;
    private final AssignAssessmentPapersUseCase assignAssessmentPapersUseCase;
    private final UpdateAssessmentCandidateStatusUseCase updateAssessmentCandidateStatusUseCase;
    private final UnblockAssessmentCandidateUseCase unblockAssessmentCandidateUseCase;
    private final DeleteAssessmentCandidateUseCase deleteAssessmentCandidateUseCase;
    private final BulkDeleteAssessmentCandidatesUseCase bulkDeleteAssessmentCandidatesUseCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentCandidateDto>> add(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody AddAssessmentCandidateRequest request) {
        AssessmentCandidateDto data = addAssessmentCandidateUseCase.execute(AddAssessmentCandidateRequest.toCommand(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Candidate added successfully", data));
    }

    @PostMapping("/import-class")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AssessmentCandidateDto>>> importClass(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody ImportAssessmentCandidatesFromClassRequest request) {
        List<AssessmentCandidateDto> data = importAssessmentCandidatesFromClassUseCase.execute(
            ImportAssessmentCandidatesFromClassRequest.toCommand(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Candidates imported by class successfully", data));
    }

    @PostMapping("/import-grade")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AssessmentCandidateDto>>> importGrade(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody ImportAssessmentCandidatesFromGradeRequest request) {
        List<AssessmentCandidateDto> data = importAssessmentCandidatesFromGradeUseCase.execute(
            ImportAssessmentCandidatesFromGradeRequest.toCommand(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Candidates imported by grade successfully", data));
    }

    @PutMapping("/{candidateId}/schedule")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentCandidateDto>> assignSchedule(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("candidateId") UUID candidateId,
            @RequestBody AssignAssessmentCandidateScheduleRequest request) {
        AssessmentCandidateDto data = assignAssessmentCandidateScheduleUseCase.execute(
            AssignAssessmentCandidateScheduleRequest.toCommand(assessmentId, candidateId, request));
        return ResponseEntity.ok(ApiResponse.success("Candidate schedule updated successfully", data));
    }

    /** Assigns or removes a whole group in one transaction -- used by the multi-select screen for candidates. */
    @PutMapping("/schedule")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AssessmentCandidateDto>>> bulkAssignSchedule(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody BulkAssignAssessmentCandidateScheduleRequest request) {
        List<AssessmentCandidateDto> data = bulkAssignAssessmentCandidateScheduleUseCase.execute(
            BulkAssignAssessmentCandidateScheduleRequest.toCommand(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Candidate schedule updated successfully", data));
    }

    @PostMapping("/auto-fill")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AssessmentCandidateDto>>> autoFill(
            @PathVariable("assessmentId") UUID assessmentId,
            @RequestBody(required = false) AutoFillAssessmentCandidatesRequest request) {
        List<AssessmentCandidateDto> data = autoFillAssessmentCandidatesUseCase.execute(
            AutoFillAssessmentCandidatesRequest.toCommand(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Candidates arranged automatically successfully", data));
    }

    @PutMapping("/assign-papers")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssignAssessmentPapersResponse>> assignPapers(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody AssignAssessmentPapersRequest request) {
        AssignAssessmentPapersResponse data = assignAssessmentPapersUseCase.execute(AssignAssessmentPapersRequest.toCommand(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Papers assigned successfully", data));
    }

    @PutMapping("/{candidateId}/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentCandidateDto>> updateStatus(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("candidateId") UUID candidateId,
            @Valid @RequestBody UpdateAssessmentCandidateStatusRequest request) {
        AssessmentCandidateDto data = updateAssessmentCandidateStatusUseCase.execute(new UpdateAssessmentCandidateStatusCommand(
            candidateId,
            request.status()
        ));
        return ResponseEntity.ok(ApiResponse.success("Candidate status updated successfully", data));
    }

    @PostMapping("/{candidateId}/unblock")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentCandidateDto>> unblock(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("candidateId") UUID candidateId,
            @Valid @RequestBody SessionReasonRequest request) {
        AssessmentCandidateDto data = unblockAssessmentCandidateUseCase.execute(new UnblockAssessmentCandidateCommand(candidateId, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Candidate unblocked successfully", data));
    }

    @DeleteMapping("/{candidateId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("candidateId") UUID candidateId) {
        deleteAssessmentCandidateUseCase.execute(new DeleteAssessmentCandidateCommand(assessmentId, candidateId));
        return ResponseEntity.ok(ApiResponse.success("Candidate removed from the assessment successfully"));
    }

    /**
     * Removes a whole group of candidates in one transaction -- used by the multi-select screen for candidates.
     *
     * <p>The ids travel in the body: a DELETE with dozens of UUIDs on the query string both hits the URL length
     * limit and dumps every id into the access log.
     */
    @DeleteMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody BulkDeleteAssessmentCandidatesRequest request) {
        bulkDeleteAssessmentCandidatesUseCase.execute(
            new BulkDeleteAssessmentCandidatesCommand(assessmentId, request.candidateIds()));
        return ResponseEntity.ok(ApiResponse.success("Candidate removed from the assessment successfully"));
    }
}
