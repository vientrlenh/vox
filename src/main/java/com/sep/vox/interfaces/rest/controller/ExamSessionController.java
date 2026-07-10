package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.CreateExamSessionCommand;
import com.sep.vox.application.port.input.command.UpdateExamSessionStatusCommand;
import com.sep.vox.application.port.input.query.ViewExamSessionFollowupsQuery;
import com.sep.vox.application.port.input.query.ViewExamSessionPaperQuery;
import com.sep.vox.application.port.input.query.ViewExamSessionQuery;
import com.sep.vox.application.port.input.usecase.exam.GetExamSessionPaperUseCase;
import com.sep.vox.application.port.input.usecase.examitemresponse.ViewExamSessionFollowupsUseCase;
import com.sep.vox.application.port.input.usecase.examsession.CreateExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionStatusUseCase;
import com.sep.vox.application.port.input.usecase.examsession.ViewExamSessionUseCase;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.interfaces.rest.dto.request.CreateExamSessionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamSessionRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/exam-sessions")
public class ExamSessionController {

    private final CreateExamSessionUseCase createExamSessionUseCase;
    private final ViewExamSessionUseCase viewExamSessionUseCase;
    private final UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase;
    private final GetExamSessionPaperUseCase getExamSessionPaperUseCase;
    private final ViewExamSessionFollowupsUseCase viewExamSessionFollowupsUseCase;

    public ExamSessionController(
            CreateExamSessionUseCase createExamSessionUseCase,
            ViewExamSessionUseCase viewExamSessionUseCase,
            UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase,
            GetExamSessionPaperUseCase getExamSessionPaperUseCase,
            ViewExamSessionFollowupsUseCase viewExamSessionFollowupsUseCase) {
        this.createExamSessionUseCase = createExamSessionUseCase;
        this.viewExamSessionUseCase = viewExamSessionUseCase;
        this.updateExamSessionStatusUseCase = updateExamSessionStatusUseCase;
        this.getExamSessionPaperUseCase = getExamSessionPaperUseCase;
        this.viewExamSessionFollowupsUseCase = viewExamSessionFollowupsUseCase;
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable UUID id) {
        var data = viewExamSessionUseCase.execute(new ViewExamSessionQuery(id));
        return ResponseEntity.ok(ApiResponse.success("Lay phien thi thanh cong", data));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExamSessionRequest request) {
        var data = updateExamSessionStatusUseCase.execute(new UpdateExamSessionStatusCommand(
            id,
            ExamSessionStatus.valueOf(request.status().trim().toUpperCase())
        ));
        return ResponseEntity.ok(ApiResponse.success("Cap nhat trang thai phien thi thanh cong", data));
    }

    @GetMapping("/{id}/paper")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getPaper(@PathVariable UUID id) {
        var data = getExamSessionPaperUseCase.execute(new ViewExamSessionPaperQuery(id));
        return ResponseEntity.ok(ApiResponse.success("Lay de thi thanh cong", data));
    }

    @GetMapping("/{id}/followups")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> getFollowups(@PathVariable UUID id) {
        var data = viewExamSessionFollowupsUseCase.execute(new ViewExamSessionFollowupsQuery(id));
        return ResponseEntity.ok(ApiResponse.success("Lay thong ke follow-up thanh cong", data));
    }
}
