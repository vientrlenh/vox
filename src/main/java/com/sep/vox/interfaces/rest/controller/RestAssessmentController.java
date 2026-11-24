package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.response.input.assessment.AssessmentEntryTicketResponse;
import com.sep.vox.application.response.input.assessment.StudentAssessmentSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.assessment.DeleteAssessmentCommand;
import com.sep.vox.application.port.input.command.assessment.DeleteAssessmentMemberCommand;
import com.sep.vox.application.port.input.command.assessment.StartClassTestSessionCommand;
import com.sep.vox.application.port.input.command.assessment.VerifyAssessmentScheduleOtpCommand;
import com.sep.vox.application.port.input.query.assessmentschedule.GetAssessmentScheduleOtpQuery;
import com.sep.vox.application.port.input.usecase.assessment.AttachAssessmentBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.assessment.CreateAssessmentMemberUseCase;
import com.sep.vox.application.port.input.usecase.assessment.CreateAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessment.DeleteAssessmentMemberUseCase;
import com.sep.vox.application.port.input.usecase.assessment.DeleteAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessment.StartClassTestSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentDeliveryModeUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentMemberUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentSecurePoolStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessment.VerifyAssessmentScheduleOtpUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewMyAssessmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.GetAssessmentScheduleOtpUseCase;
import com.sep.vox.application.response.input.assessment.DeleteAssessmentResponse;
import com.sep.vox.application.response.input.assessmentschedule.GetAssessmentScheduleOtpResponse;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.dto.AssessmentMemberDto;
import com.sep.vox.domain.dto.AssessmentSecurePoolDto;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.interfaces.rest.dto.request.assessment.AttachAssessmentBlueprintRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateAssessmentMemberRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateAssessmentRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentDeliveryModeRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentMemberRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentSecurePoolStatusRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentStatusRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.VerifyAssessmentOtpRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.ViewMyAssessmentsQueryMapper;
import com.sep.vox.interfaces.shared.PageArguments;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class RestAssessmentController {

    private final CreateAssessmentUseCase createAssessmentUseCase;
    private final UpdateAssessmentUseCase updateAssessmentUseCase;
    private final UpdateAssessmentStatusUseCase updateAssessmentStatusUseCase;
    private final DeleteAssessmentUseCase deleteAssessmentUseCase;
    private final CreateAssessmentMemberUseCase createAssessmentMemberUseCase;
    private final UpdateAssessmentMemberUseCase updateAssessmentMemberUseCase;
    private final DeleteAssessmentMemberUseCase deleteAssessmentMemberUseCase;
    private final UpdateAssessmentSecurePoolStatusUseCase updateAssessmentSecurePoolStatusUseCase;
    private final AttachAssessmentBlueprintUseCase attachAssessmentBlueprintUseCase;
    private final UpdateAssessmentDeliveryModeUseCase updateAssessmentDeliveryModeUseCase;
    private final GetAssessmentScheduleOtpUseCase getAssessmentScheduleOtpUseCase;
    private final ViewMyAssessmentsUseCase viewMyAssessmentsUseCase;
    private final VerifyAssessmentScheduleOtpUseCase verifyAssessmentScheduleOtpUseCase;
    private final StartClassTestSessionUseCase startClassTestSessionUseCase;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getMyAssessments(
            @RequestParam(name = "kind", required = false) AssessmentKind kind,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "size", defaultValue = "20") Integer size,
            @RequestParam(name = "sort", defaultValue = "assessmentDate,desc") String sort) {
        // Page numbers count from 1, like every other entry point. `Integer` rather than `int` so an empty
        // parameter (`?page=`) arrives here as null and is rejected by the guard, instead of failing at the
        // binding layer with a confusing message. Validate right at the boundary: the repository below only
        // subtracts 1 to convert to an offset.
        PageArguments.validate(page, size);
        PageResult<StudentAssessmentSummaryResponse> data = viewMyAssessmentsUseCase.execute(
            ViewMyAssessmentsQueryMapper.fromRequest(kind, status, page, size, sort));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài thi thành công", data));
    }

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentDto>> create(@Valid @RequestBody CreateAssessmentRequest request) {
        AssessmentDto data = createAssessmentUseCase.execute(CreateAssessmentRequest.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo bài kiểm tra thành công", data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentRequest request) {
        AssessmentDto data = updateAssessmentUseCase.execute(UpdateAssessmentRequest.toCommand(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài kiểm tra thành công", data));
    }

    @PatchMapping("/{id}/blueprint")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> attachBlueprint(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AttachAssessmentBlueprintRequest request) {
        AssessmentDto data = attachAssessmentBlueprintUseCase.execute(AttachAssessmentBlueprintRequest.toCommand(id, request));
        return ResponseEntity.ok(ApiResponse.success("Gắn blueprint vào bài kiểm tra thành công", data));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentStatusRequest request) {
        AssessmentDto data = updateAssessmentStatusUseCase.execute(UpdateAssessmentStatusRequest.toCommand(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái bài kiểm tra thành công", data));
    }

    @PatchMapping("/{id}/delivery-mode")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> updateDeliveryMode(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentDeliveryModeRequest request) {
        AssessmentDto data = updateAssessmentDeliveryModeUseCase.execute(UpdateAssessmentDeliveryModeRequest.toCommand(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hình thức làm bài thành công", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<DeleteAssessmentResponse>> delete(@PathVariable UUID id) {
        DeleteAssessmentResponse data = deleteAssessmentUseCase.execute(new DeleteAssessmentCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa bài kiểm tra thành công", data));
    }

    // The three council endpoints below are open to TEACHER because the council chair calls in the paper author / approver
    // for their own assessment. The real rule (school administrator OR chair, and only the school administrator may touch the
    // CHAIR row) lives in AssessmentMemberManageAccessService, not here.
    @PostMapping("/{assessmentId}/members")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentMemberDto>> createMember(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody CreateAssessmentMemberRequest request) {
        AssessmentMemberDto data = createAssessmentMemberUseCase.execute(CreateAssessmentMemberRequest.toCommand(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Gán collaborator cho bài kiểm tra thành công", data));
    }

    @PutMapping("/{assessmentId}/members/{memberId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentMemberDto>> updateMember(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("memberId") UUID memberId,
            @Valid @RequestBody UpdateAssessmentMemberRequest request) {
        AssessmentMemberDto data = updateAssessmentMemberUseCase.execute(UpdateAssessmentMemberRequest.toCommand(assessmentId, memberId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật collaborator cho bài kiểm tra thành công", data));
    }

    @DeleteMapping("/{assessmentId}/members/{memberId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("memberId") UUID memberId) {
        deleteAssessmentMemberUseCase.execute(new DeleteAssessmentMemberCommand(assessmentId, memberId));
        return ResponseEntity.ok(ApiResponse.success("Xóa collaborator khỏi bài kiểm tra thành công"));
    }

    @PatchMapping("/{assessmentId}/secure-pool/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentSecurePoolDto>> updateSecurePoolStatus(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody UpdateAssessmentSecurePoolStatusRequest request) {
        AssessmentSecurePoolDto data = updateAssessmentSecurePoolStatusUseCase.execute(
            UpdateAssessmentSecurePoolStatusRequest.toCommand(assessmentId, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Mở khoá câu hỏi đề thi thành công", data));
    }

    @GetMapping("/{assessmentId}/schedules/{scheduleId}/otp")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<GetAssessmentScheduleOtpResponse>> getAssessmentScheduleOtp(@PathVariable("assessmentId") UUID assessmentId, @PathVariable("scheduleId") UUID scheduleId) {
        GetAssessmentScheduleOtpQuery query = new GetAssessmentScheduleOtpQuery(assessmentId, scheduleId);
        GetAssessmentScheduleOtpResponse data = getAssessmentScheduleOtpUseCase.execute(query);
        return ResponseEntity.ok(ApiResponse.success("Mã OTP cho lịch thi được lấy thành công", data));
    }

    @PostMapping("/{assessmentId}/otp/verify")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> verifyAssessmentOtp(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody VerifyAssessmentOtpRequest request) {
        AssessmentEntryTicketResponse data = verifyAssessmentScheduleOtpUseCase.execute(new VerifyAssessmentScheduleOtpCommand(assessmentId, request.otp()));
        return ResponseEntity.ok(ApiResponse.success("Xác thực OTP thành công", data));
    }

    @PostMapping("/{assessmentId}/class-test/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> startClassTest(@PathVariable("assessmentId") UUID assessmentId) {
        AssessmentEntryTicketResponse data = startClassTestSessionUseCase.execute(new StartClassTestSessionCommand(assessmentId));
        return ResponseEntity.ok(ApiResponse.success("Bắt đầu bài kiểm tra thành công", data));
    }
}
