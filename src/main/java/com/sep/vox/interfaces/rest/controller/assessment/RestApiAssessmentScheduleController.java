package com.sep.vox.interfaces.rest.controller.assessment;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.DeleteAssessmentScheduleCommand;
import com.sep.vox.application.port.input.command.DeleteAssessmentScheduleProctorCommand;
import com.sep.vox.application.port.input.usecase.assessmentschedule.AddAssessmentScheduleProctorUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.CreateAssessmentScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.DeleteAssessmentScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.RemoveAssessmentScheduleProctorUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.UpdateAssessmentScheduleStatusUseCase;
import com.sep.vox.domain.dto.assessment.AssessmentScheduleDto;
import com.sep.vox.domain.dto.assessment.AssessmentScheduleProctorDto;
import com.sep.vox.interfaces.rest.dto.request.assessment.AddAssessmentScheduleProctorRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateAssessmentScheduleRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentScheduleStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/assessments/{assessmentId}/schedules")
@RequiredArgsConstructor 
public class RestApiAssessmentScheduleController {

    private final CreateAssessmentScheduleUseCase createAssessmentScheduleUseCase;
    private final UpdateAssessmentScheduleStatusUseCase updateAssessmentScheduleStatusUseCase;
    private final DeleteAssessmentScheduleUseCase deleteAssessmentScheduleUseCase;
    private final AddAssessmentScheduleProctorUseCase addAssessmentScheduleProctorUseCase;
    private final RemoveAssessmentScheduleProctorUseCase removeAssessmentScheduleProctorUseCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentScheduleDto>> create(
            @PathVariable(name = "assessmentId") UUID assessmentId,
            @Valid @RequestBody CreateAssessmentScheduleRequest request) {
        var data = createAssessmentScheduleUseCase.execute(CreateAssessmentScheduleRequest.toCommand(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo ca thi thành công", data));
    }

    @PatchMapping("/{scheduleId}/status")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentScheduleDto>> updateStatus(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("scheduleId") UUID scheduleId,
            @Valid @RequestBody UpdateAssessmentScheduleStatusRequest request) {
        var data = updateAssessmentScheduleStatusUseCase.execute(
            UpdateAssessmentScheduleStatusRequest.toCommand(assessmentId, scheduleId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái ca thi thành công", data));
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("scheduleId") UUID scheduleId) {
        deleteAssessmentScheduleUseCase.execute(new DeleteAssessmentScheduleCommand(assessmentId, scheduleId));
        return ResponseEntity.ok(ApiResponse.success("Xoá ca thi thành công"));
    }

    @PostMapping("/{scheduleId}/proctors")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentScheduleProctorDto>> addProctor(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("scheduleId") UUID scheduleId,
            @Valid @RequestBody AddAssessmentScheduleProctorRequest request) {
        var data = addAssessmentScheduleProctorUseCase.execute(
            AddAssessmentScheduleProctorRequest.toCommand(assessmentId, scheduleId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Thêm giám thị thành công", data));
    }

    @DeleteMapping("/{scheduleId}/proctors/{proctorId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> removeProctor(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("scheduleId") UUID scheduleId,
            @PathVariable("proctorId") UUID proctorId) {
        removeAssessmentScheduleProctorUseCase.execute(new DeleteAssessmentScheduleProctorCommand(assessmentId, scheduleId, proctorId));
        return ResponseEntity.ok(ApiResponse.success("Gỡ giám thị thành công"));
    }
}
