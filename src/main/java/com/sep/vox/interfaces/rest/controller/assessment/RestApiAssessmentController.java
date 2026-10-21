package com.sep.vox.interfaces.rest.controller.assessment;

import java.util.UUID;

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

import com.sep.vox.application.port.input.command.DeleteAssessmentCommand;
import com.sep.vox.application.port.input.command.DeleteAssessmentMemberCommand;
import com.sep.vox.application.port.input.command.StartClassTestSessionCommand;
import com.sep.vox.application.port.input.command.VerifyAssessmentScheduleOtpCommand;
import com.sep.vox.application.port.input.query.GetAssessmentScheduleOtpQuery;
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
import com.sep.vox.domain.dto.assessment.AssessmentDto;
import com.sep.vox.domain.dto.assessment.AssessmentMemberDto;
import com.sep.vox.domain.dto.assessment.AssessmentSecurePoolDto;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor 
public class RestApiAssessmentController {

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
        // Trang đếm từ 1, như mọi entry point khác. `Integer` chứ không phải `int` để một tham số
        // rỗng (`?page=`) vào đây thành null và bị guard từ chối, thay vì nổ ở tầng bind với thông
        // báo khó hiểu. Kiểm ngay tại biên: repository bên dưới chỉ còn trừ 1 để đổi sang offset.
        PageArguments.validate(page, size);
        var data = viewMyAssessmentsUseCase.execute(
            ViewMyAssessmentsQueryMapper.fromRequest(kind, status, page, size, sort));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài thi thành công", data));
    }

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentDto>> create(@Valid @RequestBody CreateAssessmentRequest request) {
        var data = createAssessmentUseCase.execute(CreateAssessmentRequest.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo bài kiểm tra thành công", data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentRequest request) {
        var data = updateAssessmentUseCase.execute(UpdateAssessmentRequest.toCommand(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài kiểm tra thành công", data));
    }

    @PatchMapping("/{id}/blueprint")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> attachBlueprint(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AttachAssessmentBlueprintRequest request) {
        var data = attachAssessmentBlueprintUseCase.execute(AttachAssessmentBlueprintCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Gắn blueprint vào bài kiểm tra thành công", data));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentStatusRequest request) {
        var data = updateAssessmentStatusUseCase.execute(UpdateAssessmentStatusRequest.toCommand(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái bài kiểm tra thành công", data));
    }

    @PatchMapping("/{id}/delivery-mode")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> updateDeliveryMode(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentDeliveryModeRequest request) {
        var data = updateAssessmentDeliveryModeUseCase.execute(UpdateAssessmentDeliveryModeRequest.toCommand(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hình thức làm bài thành công", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<DeleteAssessmentResponse>> delete(@PathVariable UUID id) {
        var data = deleteAssessmentUseCase.execute(new DeleteAssessmentCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa bài kiểm tra thành công", data));
    }

    // Ba endpoint hội đồng dưới đây mở cho TEACHER vì chủ tịch hội đồng tự gọi người ra đề / duyệt đề
    // vào kỳ thi của mình. Luật thật (quản trị trường HOẶC chủ tịch, và chỉ quản trị trường mới đụng
    // hàng CHAIR) nằm ở AssessmentMemberManageAccessService, không ở đây.
    @PostMapping("/{assessmentId}/members")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentMemberDto>> createMember(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody CreateAssessmentMemberRequest request) {
        var data = createAssessmentMemberUseCase.execute(CreateAssessmentMemberRequest.toCommand(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Gán collaborator cho bài kiểm tra thành công", data));
    }

    @PutMapping("/{assessmentId}/members/{memberId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentMemberDto>> updateMember(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("memberId") UUID memberId,
            @Valid @RequestBody UpdateAssessmentMemberRequest request) {
        var data = updateAssessmentMemberUseCase.execute(UpdateAssessmentMemberRequest.toCommand(assessmentId, memberId, request));
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
        var data = updateAssessmentSecurePoolStatusUseCase.execute(
            UpdateAssessmentSecurePoolStatusRequest.toCommand(assessmentId, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Mở khoá câu hỏi đề thi thành công", data));
    }

    @GetMapping("/{assessmentId}/schedules/{scheduleId}/otp")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<GetAssessmentScheduleOtpResponse>> getAssessmentScheduleOtp(@PathVariable("assessmentId") UUID assessmentId, @PathVariable("scheduleId") UUID scheduleId) {
        var query = new GetAssessmentScheduleOtpQuery(assessmentId, scheduleId);
        var data = getAssessmentScheduleOtpUseCase.execute(query);
        return ResponseEntity.ok(ApiResponse.success("Mã OTP cho lịch thi được lấy thành công", data));
    }

    @PostMapping("/{assessmentId}/otp/verify")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> verifyAssessmentOtp(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody VerifyAssessmentOtpRequest request) {
        var data = verifyAssessmentScheduleOtpUseCase.execute(new VerifyAssessmentScheduleOtpCommand(assessmentId, request.otp()));
        return ResponseEntity.ok(ApiResponse.success("Xác thực OTP thành công", data));
    }

    @PostMapping("/{assessmentId}/class-test/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> startClassTest(@PathVariable("assessmentId") UUID assessmentId) {
        var data = startClassTestSessionUseCase.execute(new StartClassTestSessionCommand(assessmentId));
        return ResponseEntity.ok(ApiResponse.success("Bắt đầu bài kiểm tra thành công", data));
    }
}
