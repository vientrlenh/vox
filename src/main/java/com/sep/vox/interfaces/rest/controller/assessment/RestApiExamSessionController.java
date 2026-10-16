package com.sep.vox.interfaces.rest.controller.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.*;

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
import com.sep.vox.application.port.input.usecase.assessment.GetExamSessionPaperUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.CompleteExamSessionGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.CreateExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.DeleteExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.FlagExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.ForceEndExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.HandOffGradingToHumanUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.ReportAiUsageUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.RetryGradingExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateExamSessionRemainingTimeUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateExamSessionStatusUseCase;
import com.sep.vox.domain.model.assessment.ExamSessionStatus;
import com.sep.vox.interfaces.rest.dto.request.ReportAiUsageRequest;
import com.sep.vox.interfaces.rest.dto.request.SessionReasonRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateRemainingTimeRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateExamSessionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateExamSessionRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/exam-sessions")
@RequiredArgsConstructor 
public class RestApiExamSessionController {

    private final CompleteExamSessionGradingUseCase completeExamSessionGradingUseCase;

    private final CreateExamSessionUseCase createExamSessionUseCase;
    private final UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase;
    private final GetExamSessionPaperUseCase getExamSessionPaperUseCase;
    private final DeleteExamSessionUseCase deleteExamSessionUseCase;
    private final FlagExamSessionUseCase flagExamSessionUseCase;
    private final ForceEndExamSessionUseCase forceEndExamSessionUseCase;
    private final RetryGradingExamSessionUseCase retryGradingExamSessionUseCase;
    private final HandOffGradingToHumanUseCase handOffGradingToHumanUseCase;
    private final UpdateExamSessionRemainingTimeUseCase updateExamSessionRemainingTimeUseCase;
    private final ReportAiUsageUseCase reportAiUsageUseCase;

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

    /**
     * Bài AI chấm lỗi -> đưa vào hàng đợi cho người chấm. Trả về id kết quả vừa tạo, để client
     * biết bài đã có mặt trong hàng đợi.
     */
    @PostMapping("/{id}/hand-off-grading")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> handOffGrading(@PathVariable("id") UUID id) {
        var data = handOffGradingToHumanUseCase.execute(new HandOffGradingToHumanCommand(id));
        return ResponseEntity.ok(ApiResponse.success(
            "Đã chuyển bài sang chấm tay, chờ nhà trường phân công giáo viên", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<?>> delete(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody SessionReasonRequest request) {
        deleteExamSessionUseCase.execute(new DeleteExamSessionCommand(id, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Xóa bài thi thành công", null));
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
