package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.response.input.assessment.StudentAssessmentPaperResponse;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
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
import com.sep.vox.application.port.input.usecase.assessmentsession.ReportAiUsageUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.HandOffGradingToHumanUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.RetryGradingAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionRemainingTimeUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionStatusUseCase;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentSessionRequest;
import com.sep.vox.interfaces.rest.dto.request.ReportAiUsageRequest;
import com.sep.vox.interfaces.rest.dto.request.SessionReasonRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentSessionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateRemainingTimeRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.ReportAiUsageCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
public class AssessmentSessionController {

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

    public AssessmentSessionController(
            CreateAssessmentSessionUseCase createAssessmentSessionUseCase,
            CompleteAssessmentSessionGradingUseCase completeAssessmentSessionGradingUseCase,
            UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase,
            GetAssessmentSessionPaperUseCase getAssessmentSessionPaperUseCase,
            DeleteAssessmentSessionUseCase deleteAssessmentSessionUseCase,
            FlagAssessmentSessionUseCase flagAssessmentSessionUseCase,
            ForceEndAssessmentSessionUseCase forceEndAssessmentSessionUseCase,
            RetryGradingAssessmentSessionUseCase retryGradingAssessmentSessionUseCase,
            HandOffGradingToHumanUseCase handOffGradingToHumanUseCase,
            UpdateAssessmentSessionRemainingTimeUseCase updateAssessmentSessionRemainingTimeUseCase,
            ReportAiUsageUseCase reportAiUsageUseCase) {
        this.createAssessmentSessionUseCase = createAssessmentSessionUseCase;
        this.completeAssessmentSessionGradingUseCase = completeAssessmentSessionGradingUseCase;
        this.updateAssessmentSessionStatusUseCase = updateAssessmentSessionStatusUseCase;
        this.getAssessmentSessionPaperUseCase = getAssessmentSessionPaperUseCase;
        this.deleteAssessmentSessionUseCase = deleteAssessmentSessionUseCase;
        this.flagAssessmentSessionUseCase = flagAssessmentSessionUseCase;
        this.forceEndAssessmentSessionUseCase = forceEndAssessmentSessionUseCase;
        this.retryGradingAssessmentSessionUseCase = retryGradingAssessmentSessionUseCase;
        this.handOffGradingToHumanUseCase = handOffGradingToHumanUseCase;
        this.updateAssessmentSessionRemainingTimeUseCase = updateAssessmentSessionRemainingTimeUseCase;
        this.reportAiUsageUseCase = reportAiUsageUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CreateAssessmentSessionRequest request) {
        AssessmentSessionResponse data = createAssessmentSessionUseCase.execute(new CreateAssessmentSessionCommand(
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
        AssessmentSessionResponse data = updateAssessmentSessionStatusUseCase.execute(new UpdateAssessmentSessionStatusCommand(
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
        Integer data = updateAssessmentSessionRemainingTimeUseCase.execute(
            new UpdateAssessmentSessionRemainingTimeCommand(id, request.remainingSeconds()));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thời gian còn lại thành công", data));
    }

    @GetMapping("/{id}/paper")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getPaper(@PathVariable("id") UUID id) {
        StudentAssessmentPaperResponse data = getAssessmentSessionPaperUseCase.execute(new ViewAssessmentSessionPaperQuery(id));
        return ResponseEntity.ok(ApiResponse.success("Lay de thi thanh cong", data));
    }

    @PostMapping("/{id}/flag")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> flag(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SessionReasonRequest request) {
        UUID data = flagAssessmentSessionUseCase.execute(new FlagAssessmentSessionCommand(id, true, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu nghi vấn phiên thi thành công", data));
    }

    @PostMapping("/{id}/force-end")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> forceEnd(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SessionReasonRequest request) {
        UUID data = forceEndAssessmentSessionUseCase.execute(new ForceEndAssessmentSessionCommand(id, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Buộc kết thúc phiên thi thành công", data));
    }

    @PostMapping("/{id}/retry-grading")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> retryGrading(@PathVariable("id") UUID id) {
        UUID data = retryGradingAssessmentSessionUseCase.execute(new RetryGradingAssessmentSessionCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Gửi yêu cầu chấm lại thành công", data));
    }

    /**
     * Bài AI chấm lỗi -> đưa vào hàng đợi cho người chấm. Trả về id kết quả vừa tạo, để client
     * biết bài đã có mặt trong hàng đợi.
     */
    @PostMapping("/{id}/hand-off-grading")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> handOffGrading(@PathVariable("id") UUID id) {
        UUID data = handOffGradingToHumanUseCase.execute(new HandOffGradingToHumanCommand(id));
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
     * A REST path parallel to the Kafka topic ai-usage-recorded -- for sources with no direct
     * Kafka connection, for example the desktop app that aggregates TTS itself on the learner's machine
     * (the desktop app, LocalAvatarSpeaker.cs) instead of going through the Agentic AI. See ReportAiUsageUseCase.
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
