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

import com.sep.vox.application.port.input.command.DeleteExamScheduleCommand;
import com.sep.vox.application.port.input.command.DeleteExamScheduleProctorCommand;
import com.sep.vox.application.port.input.usecase.assessmentschedule.AddExamScheduleProctorUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.CreateExamScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.DeleteExamScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.RemoveExamScheduleProctorUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.UpdateExamScheduleStatusUseCase;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.dto.ExamScheduleProctorDto;
import com.sep.vox.interfaces.rest.dto.request.assessment.AddExamScheduleProctorRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateExamScheduleRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateExamScheduleStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/exams/{examId}/schedules")
@RequiredArgsConstructor 
public class RestApiExamScheduleController {

    private final CreateExamScheduleUseCase createExamScheduleUseCase;
    private final UpdateExamScheduleStatusUseCase updateExamScheduleStatusUseCase;
    private final DeleteExamScheduleUseCase deleteExamScheduleUseCase;
    private final AddExamScheduleProctorUseCase addExamScheduleProctorUseCase;
    private final RemoveExamScheduleProctorUseCase removeExamScheduleProctorUseCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamScheduleDto>> create(
            @PathVariable(name = "examId") UUID examId,
            @Valid @RequestBody CreateExamScheduleRequest request) {
        var data = createExamScheduleUseCase.execute(CreateExamScheduleCommandMapper.fromRequest(examId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo ca thi thành công", data));
    }

    @PatchMapping("/{scheduleId}/status")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamScheduleDto>> updateStatus(
            @PathVariable("examId") UUID examId,
            @PathVariable("scheduleId") UUID scheduleId,
            @Valid @RequestBody UpdateExamScheduleStatusRequest request) {
        var data = updateExamScheduleStatusUseCase.execute(
            UpdateExamScheduleStatusCommandMapper.fromRequest(examId, scheduleId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái ca thi thành công", data));
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("examId") UUID examId,
            @PathVariable("scheduleId") UUID scheduleId) {
        deleteExamScheduleUseCase.execute(new DeleteExamScheduleCommand(examId, scheduleId));
        return ResponseEntity.ok(ApiResponse.success("Xoá ca thi thành công"));
    }

    @PostMapping("/{scheduleId}/proctors")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamScheduleProctorDto>> addProctor(
            @PathVariable("examId") UUID examId,
            @PathVariable("scheduleId") UUID scheduleId,
            @Valid @RequestBody AddExamScheduleProctorRequest request) {
        var data = addExamScheduleProctorUseCase.execute(
            AddExamScheduleProctorCommandMapper.fromRequest(examId, scheduleId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Thêm giám thị thành công", data));
    }

    @DeleteMapping("/{scheduleId}/proctors/{proctorId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> removeProctor(
            @PathVariable("examId") UUID examId,
            @PathVariable("scheduleId") UUID scheduleId,
            @PathVariable("proctorId") UUID proctorId) {
        removeExamScheduleProctorUseCase.execute(new DeleteExamScheduleProctorCommand(examId, scheduleId, proctorId));
        return ResponseEntity.ok(ApiResponse.success("Gỡ giám thị thành công"));
    }
}
