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

import com.sep.vox.application.port.input.command.AssignGradingCommand;
import com.sep.vox.application.port.input.usecase.assessmentgrading.AssignGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.AutoAssignGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ClearInvalidResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.DeclineGradingAssignmentUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.InvalidateResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.PreviewGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ReassignGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ReclaimOverdueAssignmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.RegradeResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.RemoveGradingAssignmentUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.SetGradingDeadlineUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.UpholdResultUseCase;
import com.sep.vox.application.response.input.assessmentgrading.GradingActionResponse;
import com.sep.vox.application.response.input.assessmentgrading.GradingPreviewResponse;
import com.sep.vox.application.response.input.assessmentgrading.ReclaimOverdueResponse;
import com.sep.vox.interfaces.rest.dto.request.GradingDecisionRequest;
import com.sep.vox.interfaces.rest.dto.request.ReclaimOverdueAssignmentsRequest;
import com.sep.vox.interfaces.rest.dto.request.SetGradingDeadlineRequest;
import com.sep.vox.interfaces.rest.dto.request.ReassignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.SubmitGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.AssignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.AutoAssignGradingRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/grading-assignments")
@RequiredArgsConstructor 
public class RestApiGradingAssignmentController {

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

    @Operation(summary = "Phân công giáo viên chấm bài (gán tay, nhiều bài một lần, một vòng chấm)")
    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> assign(
            @Valid @RequestBody AssignGradingRequest request) {
        AssignGradingCommand command = AssignGradingRequest.toCommand(request);
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
        var command = AutoAssignGradingRequest.toCommand(request);
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
        var command = ReassignGradingRequest.toCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Đổi giáo viên chấm bài thành công!", reassignGradingUseCase.execute(command)));
    }

    @Operation(summary = "Gỡ phân công chấm bài (chỉ khi chưa chấm xong)")
    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> remove(@PathVariable("assignmentId") UUID assignmentId) {
        var command = AssessmentGradingCommandMapper.toRemoveCommand(assignmentId);
        return ResponseEntity.ok(
            ApiResponse.success("Gỡ phân công thành công!", removeGradingAssignmentUseCase.execute(command)));
    }

    @Operation(summary = "Đặt / sửa hạn chấm cho một hoặc nhiều phân công. "
        + "Bỏ trống `deadlineAt` để gỡ hạn.")
    @PutMapping("/deadline")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> setDeadline(
            @Valid @RequestBody SetGradingDeadlineRequest request) {
        var command = SetGradingDeadlineRequest.toCommand(request);
        return ResponseEntity.ok(
            ApiResponse.success("Đặt hạn chấm thành công!", setGradingDeadlineUseCase.execute(command)));
    }

    @Operation(summary = "Thu hồi phân công quá hạn, giao lại ngay nếu có chọn nhóm giáo viên thay thế. "
        + "Bỏ trống `assignmentIds` để thu hồi mọi phân công quá hạn của kỳ thi. "
        + "Trả về hai danh sách tách bạch: `reclaimedAssignmentIds` (phân công cũ vừa đóng) và "
        + "`reassignedAssignmentIds` (phân công mới vừa mở, rỗng nếu chỉ thu hồi). "
        + "Mỗi lượt xử lý tối đa 500 phân công; `hasMore = true` nghĩa là còn dòng quá hạn "
        + "chưa xử lý, gọi lại để chạy lượt tiếp theo.")
    @PostMapping("/reclaim-overdue")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<ReclaimOverdueResponse>> reclaimOverdue(
            @Valid @RequestBody ReclaimOverdueAssignmentsRequest request) {
        var command = ReclaimOverdueAssignmentsRequest.toCommand(request);
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
        var command = GradingDecisionRequest.toCommand(assignmentId, request);
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
        var command = SubmitGradingRequest.toCommand(assignmentId, request);
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
        var command = SubmitGradingRequest.toCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Tính thử điểm thành công!", previewGradingUseCase.execute(command)));
    }

    @Operation(summary = "Kết luận bài là vi phạm -> vô hiệu kết quả, không nhập điểm. Lý do bắt buộc.")
    @PostMapping("/{assignmentId}/invalidate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradingActionResponse>> invalidate(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody GradingDecisionRequest request) {
        var command = GradingDecisionRequest.toCommand(assignmentId, request);
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
        var command = GradingDecisionRequest.toCommand(assignmentId, request);
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
        var command = GradingDecisionRequest.toCommand(assignmentId, request);
        return ResponseEntity.ok(
            ApiResponse.success("Đã trả lại phân công!", declineGradingAssignmentUseCase.execute(command)));
    }

}
