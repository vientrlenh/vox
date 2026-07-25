package com.sep.vox.interfaces.rest.controller;

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

import com.sep.vox.application.port.input.usecase.examgrading.AssignGradingUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.AutoAssignGradingUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.InvalidateGradingUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.PreviewGradingUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ReassignGradingUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.RemoveGradingAssignmentUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.SubmitGradingUseCase;
import com.sep.vox.application.response.input.examgrading.GradingPreviewResponse;
import com.sep.vox.application.response.input.examgrading.InvalidateGradingResponse;
import com.sep.vox.application.response.input.examgrading.SubmitGradingResponse;
import com.sep.vox.interfaces.rest.dto.request.AssignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.AutoAssignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.InvalidateGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.ReassignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.SubmitGradingRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.ExamGradingCommandMapper;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/grading-assignments")
public class GradingAssignmentController {

    private final AssignGradingUseCase assignGradingUseCase;
    private final AutoAssignGradingUseCase autoAssignGradingUseCase;
    private final ReassignGradingUseCase reassignGradingUseCase;
    private final RemoveGradingAssignmentUseCase removeGradingAssignmentUseCase;
    private final SubmitGradingUseCase submitGradingUseCase;
    private final PreviewGradingUseCase previewGradingUseCase;
    private final InvalidateGradingUseCase invalidateGradingUseCase;

    public GradingAssignmentController(
            AssignGradingUseCase assignGradingUseCase,
            AutoAssignGradingUseCase autoAssignGradingUseCase,
            ReassignGradingUseCase reassignGradingUseCase,
            RemoveGradingAssignmentUseCase removeGradingAssignmentUseCase,
            SubmitGradingUseCase submitGradingUseCase,
            PreviewGradingUseCase previewGradingUseCase,
            InvalidateGradingUseCase invalidateGradingUseCase) {
        this.assignGradingUseCase = assignGradingUseCase;
        this.autoAssignGradingUseCase = autoAssignGradingUseCase;
        this.reassignGradingUseCase = reassignGradingUseCase;
        this.removeGradingAssignmentUseCase = removeGradingAssignmentUseCase;
        this.submitGradingUseCase = submitGradingUseCase;
        this.previewGradingUseCase = previewGradingUseCase;
        this.invalidateGradingUseCase = invalidateGradingUseCase;
    }

    @Operation(summary = "Phân công giáo viên chấm bài (gán tay, nhiều bài một lần)")
    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> assign(
            @Valid @RequestBody AssignGradingRequest request) {
        var command = ExamGradingCommandMapper.fromRequest(request);
        var assignmentIds = assignGradingUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Phân công chấm bài thành công!", assignmentIds));
    }

    @Operation(summary = "Phân công tự động, chia đều theo tải hiện tại của nhóm giáo viên được chọn. "
        + "Chỉ nhận bài đang chờ chấm và chưa có người chấm.")
    @PostMapping("/auto")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> autoAssign(
            @Valid @RequestBody AutoAssignGradingRequest request) {
        var command = ExamGradingCommandMapper.fromRequest(request);
        var assignmentIds = autoAssignGradingUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Phân công tự động thành công!", assignmentIds));
    }

    @Operation(summary = "Đổi giáo viên chấm một bài (chỉ khi chưa chấm xong)")
    @PutMapping("/{assignmentId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> reassign(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody ReassignGradingRequest request) {
        var command = ExamGradingCommandMapper.fromRequest(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Đổi giáo viên chấm bài thành công!", reassignGradingUseCase.execute(command)));
    }

    @Operation(summary = "Gỡ phân công chấm bài (chỉ khi chưa chấm xong)")
    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> remove(@PathVariable("assignmentId") UUID assignmentId) {
        var command = ExamGradingCommandMapper.toRemoveCommand(assignmentId);
        return ResponseEntity.ok(
            ApiResponse.success("Gỡ phân công thành công!", removeGradingAssignmentUseCase.execute(command)));
    }

    @Operation(summary = "Giáo viên nộp điểm — nộp là chốt. Điểm nhập theo từng tiêu chí của từng phần; "
        + "tổng và xếp loại do hệ thống tính lại. Bài đang bị đánh dấu nghi vấn sẽ được gỡ cờ khi nộp.")
    @PostMapping("/{assignmentId}/grade")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SubmitGradingResponse>> grade(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody SubmitGradingRequest request) {
        var command = ExamGradingCommandMapper.fromRequest(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Nộp điểm chấm bài thành công!", submitGradingUseCase.execute(command)));
    }

    @Operation(summary = "Tính thử tổng điểm cho bộ điểm đang nhập. KHÔNG ghi gì. "
        + "Là POST vì body giống hệt /grade — tổng trả về bằng đúng tổng khi nộp.")
    @PostMapping("/{assignmentId}/grade/preview")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingPreviewResponse>> previewGrade(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody SubmitGradingRequest request) {
        var command = ExamGradingCommandMapper.fromRequest(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Tính thử điểm thành công!", previewGradingUseCase.execute(command)));
    }

    @Operation(summary = "Giáo viên kết luận bài nghi vấn là vi phạm thật -> vô hiệu kết quả, không nhập điểm")
    @PostMapping("/{assignmentId}/invalidate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<InvalidateGradingResponse>> invalidate(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody(required = false) InvalidateGradingRequest request) {
        var command = ExamGradingCommandMapper.fromRequest(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Vô hiệu bài thi thành công!", invalidateGradingUseCase.execute(command)));
    }

    // ---- Nhà trường chấm/chỉnh trực tiếp theo candidateResultId, không cần phân
    // công trước -- luôn xem/chấm được bất kỳ bài PENDING_REVIEW nào của trường mình,
    // kể cả bài chưa có phân công hoặc đang gán cho giáo viên khác. Cùng use case với
    // luồng giáo viên ở trên, chỉ khác đầu vào (candidateResultId thay vì assignmentId).

    @Operation(summary = "Nhà trường nộp điểm chấm trực tiếp cho một bài PENDING_REVIEW theo candidateResultId")
    @PostMapping("/by-result/{candidateResultId}/grade")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SubmitGradingResponse>> gradeByResult(
            @PathVariable("candidateResultId") UUID candidateResultId,
            @Valid @RequestBody SubmitGradingRequest request) {
        var command = ExamGradingCommandMapper.fromResultRequest(candidateResultId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Nộp điểm chấm bài thành công!", submitGradingUseCase.execute(command)));
    }

    @Operation(summary = "Tính thử tổng điểm cho nhà trường chấm trực tiếp theo candidateResultId. KHÔNG ghi gì.")
    @PostMapping("/by-result/{candidateResultId}/grade/preview")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<GradingPreviewResponse>> previewGradeByResult(
            @PathVariable("candidateResultId") UUID candidateResultId,
            @Valid @RequestBody SubmitGradingRequest request) {
        var command = ExamGradingCommandMapper.fromResultRequest(candidateResultId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Tính thử điểm thành công!", previewGradingUseCase.execute(command)));
    }

    @Operation(summary = "Nhà trường kết luận bài nghi vấn là vi phạm thật theo candidateResultId -> vô hiệu kết quả")
    @PostMapping("/by-result/{candidateResultId}/invalidate")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<InvalidateGradingResponse>> invalidateByResult(
            @PathVariable("candidateResultId") UUID candidateResultId,
            @Valid @RequestBody(required = false) InvalidateGradingRequest request) {
        var command = ExamGradingCommandMapper.fromResultRequest(candidateResultId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Vô hiệu bài thi thành công!", invalidateGradingUseCase.execute(command)));
    }
}
