package com.sep.vox.interfaces.rest.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.DeleteExamCommand;
import com.sep.vox.application.port.input.command.DeleteExamMemberCommand;
import com.sep.vox.application.port.input.query.GetExamScheduleOtpQuery;
import com.sep.vox.application.port.input.usecase.exam.AttachExamBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.exam.CreateExamMemberUseCase;
import com.sep.vox.application.port.input.usecase.exam.CreateExamUseCase;
import com.sep.vox.application.port.input.usecase.exam.DeleteExamMemberUseCase;
import com.sep.vox.application.port.input.usecase.exam.DeleteExamUseCase;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamDeliveryModeUseCase;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamMemberUseCase;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamSecurePoolStatusUseCase;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamStatusUseCase;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.GetExamScheduleOtpUseCase;
import com.sep.vox.application.response.input.exam.DeleteExamResponse;
import com.sep.vox.application.response.input.examschedule.GetExamScheduleOtpResponse;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.dto.ExamMemberDto;
import com.sep.vox.domain.dto.ExamSecurePoolDto;
import com.sep.vox.interfaces.rest.dto.request.AttachExamBlueprintRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateExamMemberRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateExamRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamDeliveryModeRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamMemberRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamSecurePoolStatusRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AttachExamBlueprintCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateExamCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateExamMemberCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamDeliveryModeCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamMemberCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamSecurePoolStatusCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamStatusCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/exams")
public class ExamController {

    private final CreateExamUseCase createExamUseCase;
    private final UpdateExamUseCase updateExamUseCase;
    private final UpdateExamStatusUseCase updateExamStatusUseCase;
    private final DeleteExamUseCase deleteExamUseCase;
    private final CreateExamMemberUseCase createExamMemberUseCase;
    private final UpdateExamMemberUseCase updateExamMemberUseCase;
    private final DeleteExamMemberUseCase deleteExamMemberUseCase;
    private final UpdateExamSecurePoolStatusUseCase updateExamSecurePoolStatusUseCase;
    private final AttachExamBlueprintUseCase attachExamBlueprintUseCase;
    private final UpdateExamDeliveryModeUseCase updateExamDeliveryModeUseCase;
    private final GetExamScheduleOtpUseCase getExamScheduleOtpUseCase;

    public ExamController(
            CreateExamUseCase createExamUseCase,
            UpdateExamUseCase updateExamUseCase,
            UpdateExamStatusUseCase updateExamStatusUseCase,
            DeleteExamUseCase deleteExamUseCase,
            CreateExamMemberUseCase createExamMemberUseCase,
            UpdateExamMemberUseCase updateExamMemberUseCase,
            DeleteExamMemberUseCase deleteExamMemberUseCase,
            UpdateExamSecurePoolStatusUseCase updateExamSecurePoolStatusUseCase,
            AttachExamBlueprintUseCase attachExamBlueprintUseCase,
            UpdateExamDeliveryModeUseCase updateExamDeliveryModeUseCase,
            GetExamScheduleOtpUseCase getExamScheduleOtpUseCase) {
        this.createExamUseCase = createExamUseCase;
        this.updateExamUseCase = updateExamUseCase;
        this.updateExamStatusUseCase = updateExamStatusUseCase;
        this.deleteExamUseCase = deleteExamUseCase;
        this.createExamMemberUseCase = createExamMemberUseCase;
        this.updateExamMemberUseCase = updateExamMemberUseCase;
        this.deleteExamMemberUseCase = deleteExamMemberUseCase;
        this.updateExamSecurePoolStatusUseCase = updateExamSecurePoolStatusUseCase;
        this.attachExamBlueprintUseCase = attachExamBlueprintUseCase;
        this.updateExamDeliveryModeUseCase = updateExamDeliveryModeUseCase;
        this.getExamScheduleOtpUseCase = getExamScheduleOtpUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<ExamDto>> create(@Valid @RequestBody CreateExamRequest request) {
        var data = createExamUseCase.execute(CreateExamCommandMapper.fromRequest(request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo bài kiểm tra thành công", data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExamRequest request) {
        var data = updateExamUseCase.execute(UpdateExamCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài kiểm tra thành công", data));
    }

    @PatchMapping("/{id}/blueprint")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamDto>> attachBlueprint(
            @PathVariable UUID id,
            @Valid @RequestBody AttachExamBlueprintRequest request) {
        var data = attachExamBlueprintUseCase.execute(AttachExamBlueprintCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Gắn blueprint vào bài kiểm tra thành công", data));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExamStatusRequest request) {
        var data = updateExamStatusUseCase.execute(UpdateExamStatusCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái bài kiểm tra thành công", data));
    }

    @PatchMapping("/{id}/delivery-mode")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamDto>> updateDeliveryMode(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExamDeliveryModeRequest request) {
        var data = updateExamDeliveryModeUseCase.execute(UpdateExamDeliveryModeCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hình thức làm bài thành công", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<DeleteExamResponse>> delete(@PathVariable UUID id) {
        var data = deleteExamUseCase.execute(new DeleteExamCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa bài kiểm tra thành công", data));
    }

    @PostMapping("/{examId}/members")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<ExamMemberDto>> createMember(
            @PathVariable UUID examId,
            @Valid @RequestBody CreateExamMemberRequest request) {
        var data = createExamMemberUseCase.execute(CreateExamMemberCommandMapper.fromRequest(examId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Gán collaborator cho bài kiểm tra thành công", data));
    }

    @PutMapping("/{examId}/members/{memberId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<ExamMemberDto>> updateMember(
            @PathVariable UUID examId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateExamMemberRequest request) {
        var data = updateExamMemberUseCase.execute(UpdateExamMemberCommandMapper.fromRequest(examId, memberId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật collaborator cho bài kiểm tra thành công", data));
    }

    @DeleteMapping("/{examId}/members/{memberId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            @PathVariable UUID examId,
            @PathVariable UUID memberId) {
        deleteExamMemberUseCase.execute(new DeleteExamMemberCommand(examId, memberId));
        return ResponseEntity.ok(ApiResponse.success("Xóa collaborator khỏi bài kiểm tra thành công"));
    }

    @PatchMapping("/{examId}/secure-pool/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamSecurePoolDto>> updateSecurePoolStatus(
            @PathVariable UUID examId,
            @Valid @RequestBody UpdateExamSecurePoolStatusRequest request) {
        var data = updateExamSecurePoolStatusUseCase.execute(
            UpdateExamSecurePoolStatusCommandMapper.fromRequest(examId, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Mở khoá câu hỏi đề thi thành công", data));
    }

    @GetMapping("/{examId}/schedules/{scheduleId}/otp")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GetExamScheduleOtpResponse>> getExamScheduleOtp(@PathVariable("examId") UUID examId, @PathVariable("scheduleId") UUID scheduleId) {
        var query = new GetExamScheduleOtpQuery(examId, scheduleId);
        var data = getExamScheduleOtpUseCase.execute(query);
        return ResponseEntity.ok(ApiResponse.success("Mã OTP cho lịch thi được lấy thành công", data));
    }

}
