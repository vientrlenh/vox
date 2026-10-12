package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.examappeal.ApproveAndClaimExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ApproveExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.AssignExamAppealReviewerUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.CreateExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.RejectExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.WithdrawExamAppealUseCase;
import com.sep.vox.interfaces.rest.dto.request.ApproveExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.AssignExamAppealReviewerRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.RejectExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/exam-appeals")
@RequiredArgsConstructor 
public class RestApiExamAppealController {

    private final CreateExamAppealUseCase createExamAppealUseCase;
    private final ApproveExamAppealUseCase approveExamAppealUseCase;
    private final ApproveAndClaimExamAppealUseCase approveAndClaimExamAppealUseCase;
    private final RejectExamAppealUseCase rejectExamAppealUseCase;
    private final AssignExamAppealReviewerUseCase assignExamAppealReviewerUseCase;
    private final WithdrawExamAppealUseCase withdrawExamAppealUseCase;


    @Operation(summary = "Học sinh nộp đơn phúc khảo cho một hoặc nhiều phần thi")
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<UUID>> createAppeal(
            @Valid @RequestBody CreateExamAppealRequest request) {
        var command = ExamAppealCommandMapper.fromRequest(request);
        var appealId = createExamAppealUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Gửi đơn phúc khảo thành công!", appealId));
    }

    @Operation(summary = "Học sinh rút đơn phúc khảo khi đơn còn chờ duyệt. Lượt phúc khảo được hoàn lại.")
    @PostMapping("/{appealId}/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<UUID>> withdrawAppeal(@PathVariable("appealId") UUID appealId) {
        return ResponseEntity.ok(
            ApiResponse.success("Rút đơn phúc khảo thành công!", withdrawExamAppealUseCase.execute(appealId)));
    }

    @Operation(summary = "Duyệt đơn phúc khảo và đặt hạn xử lý")
    @PostMapping("/{appealId}/approve")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> approveAppeal(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody ApproveExamAppealRequest request) {
        var command = ExamAppealCommandMapper.fromRequest(appealId, request);
        var responseId = approveExamAppealUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Duyệt đơn phúc khảo thành công!", responseId));
    }

    @Operation(summary = "Giáo viên phụ trách bài trên lớp duyệt đơn và tự nhận chấm trong một thao tác. "
        + "Hạn xử lý mặc định T+3 lúc 17:00. Trả về id dòng phân công vừa mở.")
    @PostMapping("/{appealId}/approve-and-claim")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> approveAndClaimAppeal(@PathVariable("appealId") UUID appealId) {
        return ResponseEntity.ok(ApiResponse.success(
            "Đã duyệt và nhận chấm phúc khảo!", approveAndClaimExamAppealUseCase.execute(appealId)));
    }

    @Operation(summary = "Từ chối đơn phúc khảo (bắt buộc nêu lý do)")
    @PostMapping("/{appealId}/reject")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> rejectAppeal(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody RejectExamAppealRequest request) {
        var command = ExamAppealCommandMapper.fromRequest(appealId, request);
        var responseId = rejectExamAppealUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Từ chối đơn phúc khảo thành công!", responseId));
    }

    @Operation(summary = "Giao MỘT giáo viên chấm phúc khảo. Người đã từng chấm bài này bị từ chối, "
        + "trừ khi truyền `overrideReason` — lý do đó được ghi lại trên đơn.")
    @PostMapping("/{appealId}/reviewer")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> assignReviewer(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody AssignExamAppealReviewerRequest request) {
        var command = ExamAppealCommandMapper.fromRequest(appealId, request);
        var assignmentId = assignExamAppealReviewerUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Phân công người chấm phúc khảo thành công!", assignmentId));
    }
}
