package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.examappeal.ApproveExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.AssignExamAppealReviewersUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.CreateExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.PublishExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.RejectExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.RemoveExamAppealReviewerUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.SubmitExamAppealReportUseCase;
import com.sep.vox.interfaces.rest.dto.request.ApproveExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.AssignExamAppealReviewersRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.PublishExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.RejectExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.SubmitExamAppealReportRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.ExamAppealCommandMapper;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/exam-appeals")
public class ExamAppealController {

    private final CreateExamAppealUseCase createExamAppealUseCase;
    private final ApproveExamAppealUseCase approveExamAppealUseCase;
    private final RejectExamAppealUseCase rejectExamAppealUseCase;
    private final AssignExamAppealReviewersUseCase assignExamAppealReviewersUseCase;
    private final RemoveExamAppealReviewerUseCase removeExamAppealReviewerUseCase;
    private final SubmitExamAppealReportUseCase submitExamAppealReportUseCase;
    private final PublishExamAppealUseCase publishExamAppealUseCase;

    public ExamAppealController(
            CreateExamAppealUseCase createExamAppealUseCase,
            ApproveExamAppealUseCase approveExamAppealUseCase,
            RejectExamAppealUseCase rejectExamAppealUseCase,
            AssignExamAppealReviewersUseCase assignExamAppealReviewersUseCase,
            RemoveExamAppealReviewerUseCase removeExamAppealReviewerUseCase,
            SubmitExamAppealReportUseCase submitExamAppealReportUseCase,
            PublishExamAppealUseCase publishExamAppealUseCase) {
        this.createExamAppealUseCase = createExamAppealUseCase;
        this.approveExamAppealUseCase = approveExamAppealUseCase;
        this.rejectExamAppealUseCase = rejectExamAppealUseCase;
        this.assignExamAppealReviewersUseCase = assignExamAppealReviewersUseCase;
        this.removeExamAppealReviewerUseCase = removeExamAppealReviewerUseCase;
        this.submitExamAppealReportUseCase = submitExamAppealReportUseCase;
        this.publishExamAppealUseCase = publishExamAppealUseCase;
    }

    @Operation(summary = "Học sinh nộp đơn phúc khảo cho một phần thi")
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<UUID>> createAppeal(
            @Valid @RequestBody CreateExamAppealRequest request) {
        var command = ExamAppealCommandMapper.fromRequest(request);
        var appealId = createExamAppealUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Gửi đơn phúc khảo thành công!", appealId));
    }

    @Operation(summary = "Duyệt đơn phúc khảo và đặt hạn xử lý")
    @PostMapping("/{appealId}/approve")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> approveAppeal(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody ApproveExamAppealRequest request) {
        var command = ExamAppealCommandMapper.fromRequest(appealId, request);
        var responseId = approveExamAppealUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Duyệt đơn phúc khảo thành công!", responseId));
    }

    @Operation(summary = "Từ chối đơn phúc khảo (bắt buộc nêu lý do)")
    @PostMapping("/{appealId}/reject")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> rejectAppeal(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody RejectExamAppealRequest request) {
        var command = ExamAppealCommandMapper.fromRequest(appealId, request);
        var responseId = rejectExamAppealUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Từ chối đơn phúc khảo thành công!", responseId));
    }

    @Operation(summary = "Phân công giám khảo chấm lại (1-5 người)")
    @PostMapping("/{appealId}/reviewers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> assignReviewers(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody AssignExamAppealReviewersRequest request) {
        var command = ExamAppealCommandMapper.fromRequest(appealId, request);
        var responseId = assignExamAppealReviewersUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Phân công giám khảo thành công!", responseId));
    }

    @Operation(summary = "Gỡ giám khảo chưa nộp báo cáo khỏi đơn phúc khảo")
    @DeleteMapping("/{appealId}/reviewers/{reviewerId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> removeReviewer(
            @PathVariable("appealId") UUID appealId,
            @PathVariable("reviewerId") UUID reviewerId) {
        var command = ExamAppealCommandMapper.fromRequest(appealId, reviewerId);
        var responseId = removeExamAppealReviewerUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Gỡ giám khảo thành công!", responseId));
    }

    @Operation(summary = "Giám khảo nộp báo cáo chấm lại")
    @PostMapping("/{appealId}/reviewers/me/report")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> submitReport(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody SubmitExamAppealReportRequest request) {
        var command = ExamAppealCommandMapper.fromRequest(appealId, request);
        var responseId = submitExamAppealReportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Nộp báo cáo chấm lại thành công!", responseId));
    }

    @Operation(summary = "Công bố kết quả phúc khảo. `partScore` là điểm cho phần thi được phúc khảo, "
        + "không phải điểm tổng — hệ thống tự tính lại tổng và xếp loại từ điểm này.")
    @PostMapping("/{appealId}/publish")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> publishAppeal(
            @PathVariable("appealId") UUID appealId,
            @Valid @RequestBody PublishExamAppealRequest request) {
        var command = ExamAppealCommandMapper.fromRequest(appealId, request);
        var responseId = publishExamAppealUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Công bố kết quả phúc khảo thành công!", responseId));
    }
}
