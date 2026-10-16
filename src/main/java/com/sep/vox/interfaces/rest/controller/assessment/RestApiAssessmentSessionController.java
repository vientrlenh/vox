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

import com.sep.vox.application.port.input.query.ViewAssessmentSessionPaperQuery;
import com.sep.vox.application.port.input.usecase.assessment.GetAssessmentSessionPaperUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.CompleteAssessmentSessionGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.CreateAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.DeleteAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.FlagAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.ForceEndAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.HandOffGradingToHumanUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.ReportAiUsageUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.RetryGradingAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionRemainingTimeUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionStatusUseCase;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.interfaces.rest.dto.request.ReportAiUsageRequest;
import com.sep.vox.interfaces.rest.dto.request.SessionReasonRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateRemainingTimeRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateAssessmentSessionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentSessionRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
@RequiredArgsConstructor 
public class RestApiAssessmentSessionController {

    private final CompleteAssessmentSessionGradingUseCase completeAssessmentSessionGradingUseCase;

    private final CreateAssessmentSessionUseCase createAssessmentSessionUseCase;
    private final UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase;
    private final GetAssessmentSessionPaperUseCase getAssessmentSessionPaperUseCase;
    private final DeleteAssessmentSessionUseCase deleteAssessmentSessionUseCase;
    private final FlagAssessmentSessionUseCase flagAssessmentSessionUseCase;
    private final ForceEndAssessmentSessionUseCase forceEndAssessmentSessionUseCase;
    private final RetryGradingAssessmentSessionUseCase retryGradingAssessmentSessionUseCase;
    private final HandOffGradingToHumanUseCase handOffGradingToHumanUseCase;
    private final UpdateAssessmentSessionRemainingTimeUseCase updateAssessmentSessionRemainingTimeUseCase;
    private final ReportAiUsageUseCase reportAiUsageUseCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CreateAssessmentSessionRequest request) {
        var data = createAssessmentSessionUseCase.execute(new CreateAssessmentSessionCommand(
                request.assessmentId(),
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
            @Valid @RequestBody UpdateAssessmentSessionRequest request) {
        var data = updateAssessmentSessionStatusUseCase.execute(new UpdateAssessmentSessionStatusCommand(
                id,
                AssessmentSessionStatus.valueOf(request.status().trim().toUpperCase())
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
        var data = updateAssessmentSessionRemainingTimeUseCase.execute(
            new UpdateAssessmentSessionRemainingTimeCommand(id, request.remainingSeconds()));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thời gian còn lại thành công", data));
    }

    @GetMapping("/{id}/paper")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getPaper(@PathVariable("id") UUID id) {
        var data = getAssessmentSessionPaperUseCase.execute(new ViewAssessmentSessionPaperQuery(id));
        return ResponseEntity.ok(ApiResponse.success("Lay de thi thanh cong", data));
    }

    @PostMapping("/{id}/flag")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> flag(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SessionReasonRequest request) {
        var data = flagAssessmentSessionUseCase.execute(new FlagAssessmentSessionCommand(id, true, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu nghi vấn phiên thi thành công", data));
    }

    @PostMapping("/{id}/force-end")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> forceEnd(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SessionReasonRequest request) {
        var data = forceEndAssessmentSessionUseCase.execute(new ForceEndAssessmentSessionCommand(id, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Buộc kết thúc phiên thi thành công", data));
    }

    @PostMapping("/{id}/retry-grading")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> retryGrading(@PathVariable("id") UUID id) {
        var data = retryGradingAssessmentSessionUseCase.execute(new RetryGradingAssessmentSessionCommand(id));
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
        deleteAssessmentSessionUseCase.execute(new DeleteAssessmentSessionCommand(id, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Xóa bài thi thành công", null));
    }

     @PostMapping("/{sessionId}/complete-grading")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> completeGrading(@PathVariable(name = "sessionId") UUID sessionId) {
        completeAssessmentSessionGradingUseCase.execute(new CompleteAssessmentSessionGradingCommand(sessionId));
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận hoàn tất chấm bài thành công"));
    }

    /**
     * Đường REST song song với Kafka topic ai-usage-recorded -- dùng cho nguồn không có kết nối
     * Kafka trực tiếp, ví dụ desktop app tự tổng hợp TTS ngay trên máy học viên
     * (VoxAssessmentDesktop/LocalAvatarSpeaker.cs) thay vì qua Agentic AI. Xem ReportAiUsageUseCase.
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
