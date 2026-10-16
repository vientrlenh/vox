package com.sep.vox.interfaces.rest.controller.assessment;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.assessmentapeal.ApproveAndClaimAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentapeal.ApproveAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentapeal.AssignAssessmentAppealReviewerUseCase;
import com.sep.vox.application.port.input.usecase.assessmentapeal.CreateAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentapeal.RejectAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentapeal.WithdrawAssessmentAppealUseCase;
import com.sep.vox.interfaces.rest.dto.request.assessment.ApproveAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.AssignAssessmentAppealReviewerRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.RejectAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/assessment-appeals")
@RequiredArgsConstructor 
public class RestApiAssessmentAppealController {

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
        var command = CreateAssessmentAppealRequest.toCommand(request);
        var appealId = createAssessmentAppealUseCase.execute(command);
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
        var command = ApproveAssessmentAppealRequest.toCommand(appealId, request);
        var responseId = approveAssessmentAppealUseCase.execute(command);
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
        var command = RejectAssessmentAppealRequest.toCommand(appealId, request);
        var responseId = rejectAssessmentAppealUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Từ chối đơn phúc khảo thành công!", responseId));
    }

    @Operation(summary = "Giao MỘT giáo viên chấm phúc khảo. Người đã từng chấm bài này bị từ chối, "
        + "trừ khi truyền `overrideReason` — lý do đó được ghi lại trên đơn.")
    @PostMapping("/{appealId}/reviewer")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> assignReviewer(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody AssignAssessmentAppealReviewerRequest request) {
        var command = AssignAssessmentAppealReviewerRequest.toCommand(appealId, request);
        var assignmentId = assignAssessmentAppealReviewerUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Phân công người chấm phúc khảo thành công!", assignmentId));
    }
}
