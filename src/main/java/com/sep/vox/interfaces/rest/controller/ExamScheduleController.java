package com.sep.vox.interfaces.rest.controller;

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
import com.sep.vox.application.port.input.usecase.examschedule.AddExamScheduleProctorUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.CreateExamScheduleUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.DeleteExamScheduleUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.RemoveExamScheduleProctorUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.UpdateExamScheduleStatusUseCase;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.dto.ExamScheduleProctorDto;
import com.sep.vox.interfaces.rest.dto.request.AddExamScheduleProctorRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateExamScheduleRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamScheduleStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AddExamScheduleProctorCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateExamScheduleCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamScheduleStatusCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/exams/{examId}/schedules")
public class ExamScheduleController {

    private final CreateExamScheduleUseCase createExamScheduleUseCase;
    private final UpdateExamScheduleStatusUseCase updateExamScheduleStatusUseCase;
    private final DeleteExamScheduleUseCase deleteExamScheduleUseCase;
    private final AddExamScheduleProctorUseCase addExamScheduleProctorUseCase;
    private final RemoveExamScheduleProctorUseCase removeExamScheduleProctorUseCase;

    public ExamScheduleController(
            CreateExamScheduleUseCase createExamScheduleUseCase,
            UpdateExamScheduleStatusUseCase updateExamScheduleStatusUseCase,
            DeleteExamScheduleUseCase deleteExamScheduleUseCase,
            AddExamScheduleProctorUseCase addExamScheduleProctorUseCase,
            RemoveExamScheduleProctorUseCase removeExamScheduleProctorUseCase) {
        this.createExamScheduleUseCase = createExamScheduleUseCase;
        this.updateExamScheduleStatusUseCase = updateExamScheduleStatusUseCase;
        this.deleteExamScheduleUseCase = deleteExamScheduleUseCase;
        this.addExamScheduleProctorUseCase = addExamScheduleProctorUseCase;
        this.removeExamScheduleProctorUseCase = removeExamScheduleProctorUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamScheduleDto>> create(
            @PathVariable UUID examId,
            @Valid @RequestBody CreateExamScheduleRequest request) {
        var data = createExamScheduleUseCase.execute(CreateExamScheduleCommandMapper.fromRequest(examId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo ca thi thành công", data));
    }

    @PatchMapping("/{scheduleId}/status")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamScheduleDto>> updateStatus(
            @PathVariable UUID examId,
            @PathVariable UUID scheduleId,
            @Valid @RequestBody UpdateExamScheduleStatusRequest request) {
        var data = updateExamScheduleStatusUseCase.execute(
            UpdateExamScheduleStatusCommandMapper.fromRequest(examId, scheduleId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái ca thi thành công", data));
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID examId,
            @PathVariable UUID scheduleId) {
        deleteExamScheduleUseCase.execute(new DeleteExamScheduleCommand(examId, scheduleId));
        return ResponseEntity.ok(ApiResponse.success("Xoá ca thi thành công"));
    }

    @PostMapping("/{scheduleId}/proctors")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamScheduleProctorDto>> addProctor(
            @PathVariable UUID examId,
            @PathVariable UUID scheduleId,
            @Valid @RequestBody AddExamScheduleProctorRequest request) {
        var data = addExamScheduleProctorUseCase.execute(
            AddExamScheduleProctorCommandMapper.fromRequest(examId, scheduleId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Thêm giám thị thành công", data));
    }

    @DeleteMapping("/{scheduleId}/proctors/{proctorId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> removeProctor(
            @PathVariable UUID examId,
            @PathVariable UUID scheduleId,
            @PathVariable UUID proctorId) {
        removeExamScheduleProctorUseCase.execute(new DeleteExamScheduleProctorCommand(examId, scheduleId, proctorId));
        return ResponseEntity.ok(ApiResponse.success("Gỡ giám thị thành công"));
    }
}
