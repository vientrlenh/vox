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
import com.sep.vox.application.port.input.usecase.examgrading.ClearInvalidResultUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.DeclineGradingAssignmentUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.InvalidateResultUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.PreviewGradingUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ReassignGradingUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.RegradeResultUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ReclaimOverdueAssignmentsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.RemoveGradingAssignmentUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.SetGradingDeadlineUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.UpholdResultUseCase;
import com.sep.vox.application.response.input.examgrading.GradingActionResponse;
import com.sep.vox.application.response.input.examgrading.GradingPreviewResponse;
import com.sep.vox.interfaces.rest.dto.request.AssignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.AutoAssignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.GradingDecisionRequest;
import com.sep.vox.interfaces.rest.dto.request.ReclaimOverdueAssignmentsRequest;
import com.sep.vox.interfaces.rest.dto.request.SetGradingDeadlineRequest;
import com.sep.vox.interfaces.rest.dto.request.ReassignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.SubmitGradingRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.ExamGradingCommandMapper;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

/**
 * Bốn hành động của giáo viên nằm cạnh nhau ở đây — {@code /uphold}, {@code /regrade},
 * {@code /invalidate}, {@code /clear-invalid} — dùng chung cho cả bốn vòng chấm. Vòng
 * nào cho phép hành động nào do BE quyết ({@code GradingRoundPolicy}) và trả về sẵn
 * trong {@code gradingTaskDetail.allowedOutcomes}.
 */
@RestController
@RequestMapping("/api/v1/grading-assignments")
public class GradingAssignmentController {

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

    public GradingAssignmentController(
            AssignGradingUseCase assignGradingUseCase,
            AutoAssignGradingUseCase autoAssignGradingUseCase,
            ReassignGradingUseCase reassignGradingUseCase,
            RemoveGradingAssignmentUseCase removeGradingAssignmentUseCase,
            PreviewGradingUseCase previewGradingUseCase,
            UpholdResultUseCase upholdResultUseCase,
            RegradeResultUseCase regradeResultUseCase,
            InvalidateResultUseCase invalidateResultUseCase,
            ClearInvalidResultUseCase clearInvalidResultUseCase,
            DeclineGradingAssignmentUseCase declineGradingAssignmentUseCase,
            SetGradingDeadlineUseCase setGradingDeadlineUseCase,
            ReclaimOverdueAssignmentsUseCase reclaimOverdueAssignmentsUseCase) {
        this.assignGradingUseCase = assignGradingUseCase;
        this.autoAssignGradingUseCase = autoAssignGradingUseCase;
        this.reassignGradingUseCase = reassignGradingUseCase;
        this.removeGradingAssignmentUseCase = removeGradingAssignmentUseCase;
        this.previewGradingUseCase = previewGradingUseCase;
        this.upholdResultUseCase = upholdResultUseCase;
        this.regradeResultUseCase = regradeResultUseCase;
        this.invalidateResultUseCase = invalidateResultUseCase;
        this.clearInvalidResultUseCase = clearInvalidResultUseCase;
        this.declineGradingAssignmentUseCase = declineGradingAssignmentUseCase;
        this.setGradingDeadlineUseCase = setGradingDeadlineUseCase;
        this.reclaimOverdueAssignmentsUseCase = reclaimOverdueAssignmentsUseCase;
    }

    @Operation(summary = "Phân công giáo viên chấm bài (gán tay, nhiều bài một lần, một vòng chấm)")
    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> assign(
            @Valid @RequestBody AssignGradingRequest request) {
        var command = ExamGradingCommandMapper.fromRequest(request);
        var assignmentIds = assignGradingUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Phân công chấm bài thành công!", assignmentIds));
    }

    @Operation(summary = "Phân công tự động. Chọn bài theo 4 chế độ (toàn bộ / ngẫu nhiên N% / "
        + "rủi ro cao / danh sách chỉ định) rồi chia đều theo tải hiện tại của nhóm giáo viên.")
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

    @Operation(summary = "Đặt / sửa hạn chấm cho một hoặc nhiều phân công. "
        + "Bỏ trống `deadlineAt` để gỡ hạn.")
    @PutMapping("/deadline")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> setDeadline(
            @Valid @RequestBody SetGradingDeadlineRequest request) {
        var command = ExamGradingCommandMapper.fromRequest(request);
        return ResponseEntity.ok(
            ApiResponse.success("Đặt hạn chấm thành công!", setGradingDeadlineUseCase.execute(command)));
    }

    @Operation(summary = "Thu hồi phân công quá hạn, giao lại ngay nếu có chọn nhóm giáo viên thay thế. "
        + "Bỏ trống `assignmentIds` để thu hồi mọi phân công quá hạn của kỳ thi.")
    @PostMapping("/reclaim-overdue")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> reclaimOverdue(
            @Valid @RequestBody ReclaimOverdueAssignmentsRequest request) {
        var command = ExamGradingCommandMapper.fromRequest(request);
        return ResponseEntity.ok(
            ApiResponse.success("Thu hồi phân công quá hạn thành công!",
                reclaimOverdueAssignmentsUseCase.execute(command)));
    }

    @Operation(summary = "Giữ nguyên điểm — giáo viên xác nhận điểm đang có là đúng. "
        + "Ở vòng chấm lần đầu, bài được công bố luôn (không cần admin duyệt lại).")
    @PostMapping("/{assignmentId}/uphold")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingActionResponse>> uphold(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody(required = false) GradingDecisionRequest request) {
        var command = ExamGradingCommandMapper.toDecisionCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Đã giữ nguyên điểm bài thi!", upholdResultUseCase.execute(command)));
    }

    @Operation(summary = "Chấm lại — nộp là chốt. Điểm nhập theo từng tiêu chí của từng phần; "
        + "tổng và xếp loại do hệ thống tính lại. Bài đang bị đánh dấu nghi vấn sẽ được gỡ cờ khi nộp.")
    @PostMapping("/{assignmentId}/regrade")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingActionResponse>> regrade(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody SubmitGradingRequest request) {
        var command = ExamGradingCommandMapper.fromRequest(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Nộp điểm chấm bài thành công!", regradeResultUseCase.execute(command)));
    }

    @Operation(summary = "Tính thử tổng điểm cho bộ điểm đang nhập. KHÔNG ghi gì. "
        + "Là POST vì body giống hệt /regrade — tổng trả về bằng đúng tổng khi nộp.")
    @PostMapping("/{assignmentId}/regrade/preview")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingPreviewResponse>> previewRegrade(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody SubmitGradingRequest request) {
        var command = ExamGradingCommandMapper.fromRequest(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Tính thử điểm thành công!", previewGradingUseCase.execute(command)));
    }

    @Operation(summary = "Kết luận bài là vi phạm -> vô hiệu kết quả, không nhập điểm. Lý do bắt buộc.")
    @PostMapping("/{assignmentId}/invalidate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingActionResponse>> invalidate(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody GradingDecisionRequest request) {
        var command = ExamGradingCommandMapper.toDecisionCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Vô hiệu bài thi thành công!", invalidateResultUseCase.execute(command)));
    }

    @Operation(summary = "Kết luận KHÔNG vi phạm -> gỡ vô hiệu, gỡ chặn thí sinh, và mở luôn "
        + "vòng chấm lần đầu cho chính giáo viên này. Lý do bắt buộc.")
    @PostMapping("/{assignmentId}/clear-invalid")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingActionResponse>> clearInvalid(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody GradingDecisionRequest request) {
        var command = ExamGradingCommandMapper.toDecisionCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Đã gỡ vô hiệu bài thi!", clearInvalidResultUseCase.execute(command)));
    }

    @Operation(summary = "Trả lại phân công kèm lý do (quen biết thí sinh...). "
        + "Bài quay về hàng chưa giao, admin nhận thông báo.")
    @PostMapping("/{assignmentId}/decline")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingActionResponse>> decline(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody GradingDecisionRequest request) {
        var command = ExamGradingCommandMapper.toDecisionCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Đã trả lại phân công!", declineGradingAssignmentUseCase.execute(command)));
    }
}
