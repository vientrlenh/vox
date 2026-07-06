package com.sep.vox.interfaces.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.examcandidate.AddExamCandidateUseCase;
import com.sep.vox.application.port.input.usecase.examcandidate.AssignExamCandidateScheduleUseCase;
import com.sep.vox.application.port.input.usecase.examcandidate.AssignExamPapersUseCase;
import com.sep.vox.application.port.input.usecase.examcandidate.AutoFillExamCandidatesUseCase;
import com.sep.vox.application.port.input.usecase.examcandidate.ImportExamCandidatesFromClassUseCase;
import com.sep.vox.application.response.input.examcandidate.AssignExamPapersResponse;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.interfaces.rest.dto.request.AddExamCandidateRequest;
import com.sep.vox.interfaces.rest.dto.request.AssignExamCandidateScheduleRequest;
import com.sep.vox.interfaces.rest.dto.request.AssignExamPapersRequest;
import com.sep.vox.interfaces.rest.dto.request.AutoFillExamCandidatesRequest;
import com.sep.vox.interfaces.rest.dto.request.ImportExamCandidatesFromClassRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AddExamCandidateCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AssignExamCandidateScheduleCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AssignExamPapersCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AutoFillExamCandidatesCommandMapper;
import com.sep.vox.interfaces.rest.mapper.ImportExamCandidatesFromClassCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/exams/{examId}/candidates")
public class ExamCandidateController {

    private final AddExamCandidateUseCase addExamCandidateUseCase;
    private final ImportExamCandidatesFromClassUseCase importExamCandidatesFromClassUseCase;
    private final AssignExamCandidateScheduleUseCase assignExamCandidateScheduleUseCase;
    private final AutoFillExamCandidatesUseCase autoFillExamCandidatesUseCase;
    private final AssignExamPapersUseCase assignExamPapersUseCase;

    public ExamCandidateController(
            AddExamCandidateUseCase addExamCandidateUseCase,
            ImportExamCandidatesFromClassUseCase importExamCandidatesFromClassUseCase,
            AssignExamCandidateScheduleUseCase assignExamCandidateScheduleUseCase,
            AutoFillExamCandidatesUseCase autoFillExamCandidatesUseCase,
            AssignExamPapersUseCase assignExamPapersUseCase) {
        this.addExamCandidateUseCase = addExamCandidateUseCase;
        this.importExamCandidatesFromClassUseCase = importExamCandidatesFromClassUseCase;
        this.assignExamCandidateScheduleUseCase = assignExamCandidateScheduleUseCase;
        this.autoFillExamCandidatesUseCase = autoFillExamCandidatesUseCase;
        this.assignExamPapersUseCase = assignExamPapersUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamCandidateDto>> add(
            @PathVariable UUID examId,
            @Valid @RequestBody AddExamCandidateRequest request) {
        var data = addExamCandidateUseCase.execute(AddExamCandidateCommandMapper.fromRequest(examId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Thêm thí sinh thành công", data));
    }

    @PostMapping("/import-class")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ExamCandidateDto>>> importClass(
            @PathVariable UUID examId,
            @Valid @RequestBody ImportExamCandidatesFromClassRequest request) {
        var data = importExamCandidatesFromClassUseCase.execute(
            ImportExamCandidatesFromClassCommandMapper.fromRequest(examId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Nhập thí sinh theo lớp thành công", data));
    }

    @PutMapping("/{candidateId}/schedule")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ExamCandidateDto>> assignSchedule(
            @PathVariable UUID examId,
            @PathVariable UUID candidateId,
            @RequestBody AssignExamCandidateScheduleRequest request) {
        var data = assignExamCandidateScheduleUseCase.execute(
            AssignExamCandidateScheduleCommandMapper.fromRequest(examId, candidateId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ca thi của thí sinh thành công", data));
    }

    @PostMapping("/auto-fill")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ExamCandidateDto>>> autoFill(
            @PathVariable UUID examId,
            @RequestBody(required = false) AutoFillExamCandidatesRequest request) {
        var data = autoFillExamCandidatesUseCase.execute(
            AutoFillExamCandidatesCommandMapper.fromRequest(examId, request));
        return ResponseEntity.ok(ApiResponse.success("Tự động xếp thí sinh thành công", data));
    }

    @PutMapping("/assign-papers")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AssignExamPapersResponse>> assignPapers(
            @PathVariable UUID examId,
            @Valid @RequestBody AssignExamPapersRequest request) {
        var data = assignExamPapersUseCase.execute(AssignExamPapersCommandMapper.fromRequest(examId, request));
        return ResponseEntity.ok(ApiResponse.success("Phân đề thành công", data));
    }
}
