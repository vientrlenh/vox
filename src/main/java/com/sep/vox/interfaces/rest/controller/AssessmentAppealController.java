package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.ApproveAssessmentAppealCommand;
import com.sep.vox.application.port.input.command.AssignAssessmentAppealReviewerCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentAppealCommand;
import com.sep.vox.application.port.input.command.RejectAssessmentAppealCommand;
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
import com.sep.vox.interfaces.rest.dto.request.ApproveAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.AssignAssessmentAppealReviewerRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.RejectAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AssessmentAppealCommandMapper;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

/**
 * Admin chỉ còn ĐIỀU PHỐI đơn: duyệt / từ chối / giao người chấm.
 *
 * <p>Hai endpoint cũ đã bị gỡ: {@code /reviewers/me/report} và {@code /publish}.
 * Người chấm phúc khảo nay dùng chung màn chấm với ba vòng còn lại
 * ({@code /api/v1/grading-assignments/{id}/uphold|regrade}), và nộp là công bố luôn —
 * không còn bước admin đối chiếu rồi bấm publish.
 */
@RestController
@RequestMapping("/api/v1/assessment-appeals")
@RequiredArgsConstructor
public class AssessmentAppealController {

    private final CreateAssessmentAppealUseCase createAssessmentAppealUseCase;
    private final ApproveAssessmentAppealUseCase approveAssessmentAppealUseCase;
    private final ApproveAndClaimAssessmentAppealUseCase approveAndClaimAssessmentAppealUseCase;
    private final RejectAssessmentAppealUseCase rejectAssessmentAppealUseCase;
    private final AssignAssessmentAppealReviewerUseCase assignAssessmentAppealReviewerUseCase;
    private final WithdrawAssessmentAppealUseCase withdrawAssessmentAppealUseCase;

    @Operation(summary = "Học sinh nộp đơn phúc khảo cho một hoặc nhiều phần thi")
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<UUID>> createAppeal(
            @Valid @RequestBody CreateAssessmentAppealRequest request) {
        CreateAssessmentAppealCommand command = AssessmentAppealCommandMapper.fromRequest(request);
        UUID appealId = createAssessmentAppealUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Gửi đơn phúc khảo thành công!", appealId));
    }

    @Operation(summary = "Học sinh rút đơn phúc khảo khi đơn còn chờ duyệt. Lượt phúc khảo được hoàn lại.")
    @PostMapping("/{appealId}/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<UUID>> withdrawAppeal(@PathVariable("appealId") UUID appealId) {
        return ResponseEntity.ok(
            ApiResponse.success("Rút đơn phúc khảo thành công!", withdrawAssessmentAppealUseCase.execute(appealId)));
    }

    @Operation(summary = "Duyệt đơn phúc khảo và đặt hạn xử lý")
    @PostMapping("/{appealId}/approve")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> approveAppeal(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody ApproveAssessmentAppealRequest request) {
        ApproveAssessmentAppealCommand command = AssessmentAppealCommandMapper.fromRequest(appealId, request);
        UUID responseId = approveAssessmentAppealUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Duyệt đơn phúc khảo thành công!", responseId));
    }

    @Operation(summary = "Giáo viên phụ trách bài trên lớp duyệt đơn và tự nhận chấm trong một thao tác. "
        + "Hạn xử lý mặc định T+3 lúc 17:00. Trả về id dòng phân công vừa mở.")
    @PostMapping("/{appealId}/approve-and-claim")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> approveAndClaimAppeal(@PathVariable("appealId") UUID appealId) {
        return ResponseEntity.ok(ApiResponse.success(
            "Đã duyệt và nhận chấm phúc khảo!", approveAndClaimAssessmentAppealUseCase.execute(appealId)));
    }

    @Operation(summary = "Từ chối đơn phúc khảo (bắt buộc nêu lý do)")
    @PostMapping("/{appealId}/reject")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> rejectAppeal(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody RejectAssessmentAppealRequest request) {
        RejectAssessmentAppealCommand command = AssessmentAppealCommandMapper.fromRequest(appealId, request);
        UUID responseId = rejectAssessmentAppealUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Từ chối đơn phúc khảo thành công!", responseId));
    }

    @Operation(summary = "Giao MỘT giáo viên chấm phúc khảo. Người đã từng chấm bài này bị từ chối, "
        + "trừ khi truyền `overrideReason` — lý do đó được ghi lại trên đơn.")
    @PostMapping("/{appealId}/reviewer")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> assignReviewer(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody AssignAssessmentAppealReviewerRequest request) {
        AssignAssessmentAppealReviewerCommand command = AssessmentAppealCommandMapper.fromRequest(appealId, request);
        UUID assignmentId = assignAssessmentAppealReviewerUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Phân công người chấm phúc khảo thành công!", assignmentId));
    }
}
