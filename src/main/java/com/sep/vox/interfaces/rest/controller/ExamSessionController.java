package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import com.sep.vox.application.port.input.command.*;
import com.sep.vox.application.port.input.usecase.exam.CompleteExamSessionGradingUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.query.ViewExamSessionPaperQuery;
import com.sep.vox.application.port.input.usecase.exam.GetExamSessionPaperUseCase;
import com.sep.vox.application.port.input.usecase.examsession.CreateExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.DeleteExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.FlagExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.ForceEndExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.ReportAiUsageUseCase;
import com.sep.vox.application.port.input.usecase.examsession.RetryGradingExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionRemainingTimeUseCase;
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionStatusUseCase;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.interfaces.rest.dto.request.CreateExamSessionRequest;
import com.sep.vox.interfaces.rest.dto.request.ReportAiUsageRequest;
import com.sep.vox.interfaces.rest.dto.request.SessionReasonRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamSessionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateRemainingTimeRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.ReportAiUsageCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/exam-sessions")
public class ExamSessionController {

    private final CompleteExamSessionGradingUseCase completeExamSessionGradingUseCase;

    private final CreateExamSessionUseCase createExamSessionUseCase;
    private final UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase;
    private final GetExamSessionPaperUseCase getExamSessionPaperUseCase;
    private final DeleteExamSessionUseCase deleteExamSessionUseCase;
    private final FlagExamSessionUseCase flagExamSessionUseCase;
    private final ForceEndExamSessionUseCase forceEndExamSessionUseCase;
    private final RetryGradingExamSessionUseCase retryGradingExamSessionUseCase;
    private final UpdateExamSessionRemainingTimeUseCase updateExamSessionRemainingTimeUseCase;
    private final ReportAiUsageUseCase reportAiUsageUseCase;

    public ExamSessionController(
            CreateExamSessionUseCase createExamSessionUseCase,
            CompleteExamSessionGradingUseCase completeExamSessionGradingUseCase,
            UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase,
            GetExamSessionPaperUseCase getExamSessionPaperUseCase,
            DeleteExamSessionUseCase deleteExamSessionUseCase,
            FlagExamSessionUseCase flagExamSessionUseCase,
            ForceEndExamSessionUseCase forceEndExamSessionUseCase,
            RetryGradingExamSessionUseCase retryGradingExamSessionUseCase,
            UpdateExamSessionRemainingTimeUseCase updateExamSessionRemainingTimeUseCase,
            ReportAiUsageUseCase reportAiUsageUseCase) {
        this.createExamSessionUseCase = createExamSessionUseCase;
        this.completeExamSessionGradingUseCase = completeExamSessionGradingUseCase;
        this.updateExamSessionStatusUseCase = updateExamSessionStatusUseCase;
        this.getExamSessionPaperUseCase = getExamSessionPaperUseCase;
        this.deleteExamSessionUseCase = deleteExamSessionUseCase;
        this.flagExamSessionUseCase = flagExamSessionUseCase;
        this.forceEndExamSessionUseCase = forceEndExamSessionUseCase;
        this.retryGradingExamSessionUseCase = retryGradingExamSessionUseCase;
        this.updateExamSessionRemainingTimeUseCase = updateExamSessionRemainingTimeUseCase;
        this.reportAiUsageUseCase = reportAiUsageUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CreateExamSessionRequest request) {
        var data = createExamSessionUseCase.execute(new CreateExamSessionCommand(
                request.examId(),
                request.candidateId(),
                request.paperId()
        ));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tao phien thi thanh cong", data));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateExamSessionRequest request) {
        var data = updateExamSessionStatusUseCase.execute(new UpdateExamSessionStatusCommand(
                id,
                ExamSessionStatus.valueOf(request.status().trim().toUpperCase())
        ));
        return ResponseEntity.ok(ApiResponse.success("Cap nhat trang thai phien thi thanh cong", data));
    }

    /**
     * Ghi lại đồng hồ đếm ngược của client (mặc định 10 giây một lần) để lần vào lại phiên thi
     * không bắt đầu lại từ đầu. Trả về giá trị server thực sự đang giữ, có thể nhỏ hơn giá trị gửi
     * lên nếu nó bị chặn trên hoặc bị từ chối vì không đi lùi.
     */
    @PatchMapping("/{id}/remaining-time")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Integer>> updateRemainingTime(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateRemainingTimeRequest request) {
        var data = updateExamSessionRemainingTimeUseCase.execute(
            new UpdateExamSessionRemainingTimeCommand(id, request.remainingSeconds()));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thời gian còn lại thành công", data));
    }

    @GetMapping("/{id}/paper")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getPaper(@PathVariable("id") UUID id) {
        var data = getExamSessionPaperUseCase.execute(new ViewExamSessionPaperQuery(id));
        return ResponseEntity.ok(ApiResponse.success("Lay de thi thanh cong", data));
    }

    @PostMapping("/{id}/flag")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> flag(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SessionReasonRequest request) {
        var data = flagExamSessionUseCase.execute(new FlagExamSessionCommand(id, true, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu nghi vấn phiên thi thành công", data));
    }

    @PostMapping("/{id}/force-end")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> forceEnd(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SessionReasonRequest request) {
        var data = forceEndExamSessionUseCase.execute(new ForceEndExamSessionCommand(id, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Buộc kết thúc phiên thi thành công", data));
    }

    @PostMapping("/{id}/retry-grading")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> retryGrading(@PathVariable("id") UUID id) {
        var data = retryGradingExamSessionUseCase.execute(new RetryGradingExamSessionCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Gửi yêu cầu chấm lại thành công", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable(name = "id") UUID id) {
        deleteExamSessionUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success("Xoa phien thi thanh cong", null));
    }

     @PostMapping("/{sessionId}/complete-grading")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> completeGrading(@PathVariable(name = "sessionId") UUID sessionId) {
        completeExamSessionGradingUseCase.execute(new CompleteExamSessionGradingCommand(sessionId));
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận hoàn tất chấm bài thành công"));
    }

    /**
     * Đường REST song song với Kafka topic ai-usage-recorded -- dùng cho nguồn không có kết nối
     * Kafka trực tiếp, ví dụ desktop app tự tổng hợp TTS ngay trên máy học viên
     * (VoxExamDesktop/LocalAvatarSpeaker.cs) thay vì qua Agentic AI. Xem ReportAiUsageUseCase.
     */
    @PostMapping("/{id}/ai-usage")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> reportAiUsage(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ReportAiUsageRequest request) {
        reportAiUsageUseCase.execute(ReportAiUsageCommandMapper.toCommand(id, request));
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận chi phí AI thành công"));
    }
}
