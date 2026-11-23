package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.assessmentappeal.ApproveAssessmentAppealCommand;
import com.sep.vox.application.port.input.command.assessmentappeal.AssignAssessmentAppealReviewerCommand;
import com.sep.vox.application.port.input.command.assessmentappeal.CreateAssessmentAppealCommand;
import com.sep.vox.application.port.input.command.assessmentappeal.RejectAssessmentAppealCommand;
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
import com.sep.vox.interfaces.rest.dto.request.assessmentappeal.ApproveAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentappeal.AssignAssessmentAppealReviewerRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentappeal.CreateAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentappeal.RejectAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

/**
 * The admin now only COORDINATES appeals: approve / reject / assign a grader.
 *
 * <p>Two old endpoints were removed: {@code /reviewers/me/report} and {@code /publish}.
 * Appeal graders now share the grading screen with the other three rounds
 * ({@code /api/v1/grading-assignments/{id}/uphold|regrade}), and submitting publishes the
 * result right away -- there is no longer an admin step that cross-checks and presses publish.
 */
@RestController
@RequestMapping("/api/v1/assessment-appeals")
@RequiredArgsConstructor
public class RestAssessmentAppealController {

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
        CreateAssessmentAppealCommand command = CreateAssessmentAppealRequest.toCommand(request);
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
        ApproveAssessmentAppealCommand command = ApproveAssessmentAppealRequest.toCommand(appealId, request);
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
        RejectAssessmentAppealCommand command = RejectAssessmentAppealRequest.toCommand(appealId, request);
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
        AssignAssessmentAppealReviewerCommand command = AssignAssessmentAppealReviewerRequest.toCommand(appealId, request);
        UUID assignmentId = assignAssessmentAppealReviewerUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Phân công người chấm phúc khảo thành công!", assignmentId));
    }
}
