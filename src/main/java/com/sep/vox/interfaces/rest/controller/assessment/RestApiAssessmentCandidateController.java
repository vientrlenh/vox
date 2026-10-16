package com.sep.vox.interfaces.rest.controller.assessment;

import java.util.List;
import java.util.UUID;

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

import com.sep.vox.application.port.input.command.BulkDeleteAssessmentCandidatesCommand;
import com.sep.vox.application.port.input.command.DeleteAssessmentCandidateCommand;
import com.sep.vox.application.port.input.command.UnblockAssessmentCandidateCommand;
import com.sep.vox.application.port.input.command.UpdateAssessmentCandidateStatusCommand;
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
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.interfaces.rest.dto.request.BulkDeleteAssessmentCandidatesRequest;
import com.sep.vox.interfaces.rest.dto.request.ImportAssessmentCandidatesFromClassRequest;
import com.sep.vox.interfaces.rest.dto.request.ImportAssessmentCandidatesFromGradeRequest;
import com.sep.vox.interfaces.rest.dto.request.SessionReasonRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.AddAssessmentCandidateRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.AssignAssessmentCandidateScheduleRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.AssignAssessmentPapersRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.AutoFillAssessmentCandidatesRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.BulkAssignAssessmentCandidateScheduleRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentCandidateStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/assessments/{assessmentId}/candidates")
@RequiredArgsConstructor 
public class RestApiAssessmentCandidateController {

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
        var data = addAssessmentCandidateUseCase.execute(AddAssessmentCandidateCommandMapper.fromRequest(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Thêm thí sinh thành công", data));
    }

    @PostMapping("/import-class")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AssessmentCandidateDto>>> importClass(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody ImportAssessmentCandidatesFromClassRequest request) {
        var data = importAssessmentCandidatesFromClassUseCase.execute(
            ImportAssessmentCandidatesFromClassCommandMapper.fromRequest(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Nhập thí sinh theo lớp thành công", data));
    }

    @PostMapping("/import-grade")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AssessmentCandidateDto>>> importGrade(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody ImportAssessmentCandidatesFromGradeRequest request) {
        var data = importAssessmentCandidatesFromGradeUseCase.execute(
            ImportAssessmentCandidatesFromGradeCommandMapper.fromRequest(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Nhập thí sinh theo khối thành công", data));
    }

    @PutMapping("/{candidateId}/schedule")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentCandidateDto>> assignSchedule(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("candidateId") UUID candidateId,
            @RequestBody AssignAssessmentCandidateScheduleRequest request) {
        var data = assignAssessmentCandidateScheduleUseCase.execute(
            AssignAssessmentCandidateScheduleCommandMapper.fromRequest(assessmentId, candidateId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ca thi của thí sinh thành công", data));
    }

    /** Xếp/gỡ cả nhóm trong một transaction — dùng cho màn tick chọn nhiều thí sinh. */
    @PutMapping("/schedule")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AssessmentCandidateDto>>> bulkAssignSchedule(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody BulkAssignAssessmentCandidateScheduleRequest request) {
        var data = bulkAssignAssessmentCandidateScheduleUseCase.execute(
            BulkAssignAssessmentCandidateScheduleCommandMapper.fromRequest(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ca thi của thí sinh thành công", data));
    }

    @PostMapping("/auto-fill")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AssessmentCandidateDto>>> autoFill(
            @PathVariable("assessmentId") UUID assessmentId,
            @RequestBody(required = false) AutoFillAssessmentCandidatesRequest request) {
        var data = autoFillAssessmentCandidatesUseCase.execute(
            AutoFillAssessmentCandidatesCommandMapper.fromRequest(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Tự động xếp thí sinh thành công", data));
    }

    @PutMapping("/assign-papers")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssignAssessmentPapersResponse>> assignPapers(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody AssignAssessmentPapersRequest request) {
        var data = assignAssessmentPapersUseCase.execute(AssignAssessmentPapersCommandMapper.fromRequest(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Phân đề thành công", data));
    }

    @PutMapping("/{candidateId}/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentCandidateDto>> updateStatus(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("candidateId") UUID candidateId,
            @Valid @RequestBody UpdateAssessmentCandidateStatusRequest request) {
        var data = updateAssessmentCandidateStatusUseCase.execute(new UpdateAssessmentCandidateStatusCommand(
            candidateId,
            AssessmentCandidateStatus.valueOf(request.status().trim().toUpperCase())
        ));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thí sinh thành công", data));
    }

    @PostMapping("/{candidateId}/unblock")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentCandidateDto>> unblock(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("candidateId") UUID candidateId,
            @Valid @RequestBody SessionReasonRequest request) {
        var data = unblockAssessmentCandidateUseCase.execute(new UnblockAssessmentCandidateCommand(candidateId, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Dỡ chặn thí sinh thành công", data));
    }

    @DeleteMapping("/{candidateId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("candidateId") UUID candidateId) {
        deleteAssessmentCandidateUseCase.execute(new DeleteAssessmentCandidateCommand(assessmentId, candidateId));
        return ResponseEntity.ok(ApiResponse.success("Xóa thí sinh khỏi kỳ thi thành công"));
    }

    /**
     * Xoá cả nhóm thí sinh trong một transaction — dùng cho màn tick chọn nhiều thí sinh.
     *
     * <p>Danh sách id đi trong body: DELETE với hàng chục UUID trên query string vừa chạm giới hạn
     * độ dài URL vừa đổ hết id vào access log.
     */
    @DeleteMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> bulkDelete(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody BulkDeleteAssessmentCandidatesRequest request) {
        bulkDeleteAssessmentCandidatesUseCase.execute(
            new BulkDeleteAssessmentCandidatesCommand(assessmentId, request.candidateIds()));
        return ResponseEntity.ok(ApiResponse.success("Xóa thí sinh khỏi kỳ thi thành công"));
    }
}
