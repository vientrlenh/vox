package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.UpdateExamItemResponseTurnCommand;
import com.sep.vox.application.port.input.query.ViewExamItemResponseEvaluationQuery;
import com.sep.vox.application.port.input.query.ViewExamItemResponseQuery;
import com.sep.vox.application.port.input.query.ViewExamItemResponseTurnsQuery;
import com.sep.vox.application.port.input.usecase.examevaluation.ViewExamItemResponseEvaluationUseCase;
import com.sep.vox.application.port.input.usecase.examitemresponse.UpdateExamItemResponseTurnUseCase;
import com.sep.vox.application.port.input.usecase.examitemresponse.ViewExamItemResponseTurnsUseCase;
import com.sep.vox.application.port.input.usecase.examitemresponse.ViewExamItemResponseUseCase;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamItemResponseTurnRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/exam-item-responses")
public class ExamItemResponseController {

    private final ViewExamItemResponseUseCase viewExamItemResponseUseCase;
    private final ViewExamItemResponseTurnsUseCase viewExamItemResponseTurnsUseCase;
    private final UpdateExamItemResponseTurnUseCase updateExamItemResponseTurnUseCase;
    private final ViewExamItemResponseEvaluationUseCase viewExamItemResponseEvaluationUseCase;

    public ExamItemResponseController(
            ViewExamItemResponseUseCase viewExamItemResponseUseCase,
            ViewExamItemResponseTurnsUseCase viewExamItemResponseTurnsUseCase,
            UpdateExamItemResponseTurnUseCase updateExamItemResponseTurnUseCase,
            ViewExamItemResponseEvaluationUseCase viewExamItemResponseEvaluationUseCase) {
        this.viewExamItemResponseUseCase = viewExamItemResponseUseCase;
        this.viewExamItemResponseTurnsUseCase = viewExamItemResponseTurnsUseCase;
        this.updateExamItemResponseTurnUseCase = updateExamItemResponseTurnUseCase;
        this.viewExamItemResponseEvaluationUseCase = viewExamItemResponseEvaluationUseCase;
    }

    @GetMapping("/{answerId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable UUID answerId) {
        var data = viewExamItemResponseUseCase.execute(new ViewExamItemResponseQuery(answerId));
        return ResponseEntity.ok(ApiResponse.success("Lay cau tra loi thanh cong", data));
    }

    @GetMapping("/{answerId}/turns")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> getTurns(@PathVariable UUID answerId) {
        var data = viewExamItemResponseTurnsUseCase.execute(new ViewExamItemResponseTurnsQuery(answerId));
        return ResponseEntity.ok(ApiResponse.success("Lay danh sach turn thanh cong", data));
    }

    @GetMapping("/{answerId}/evaluation")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> getEvaluation(@PathVariable UUID answerId) {
        var data = viewExamItemResponseEvaluationUseCase.execute(new ViewExamItemResponseEvaluationQuery(answerId));
        return ResponseEntity.ok(ApiResponse.success("Lay evaluation thanh cong", data));
    }

    @PatchMapping("/{answerId}/turns/{turnOrder}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> updateTurn(
            @PathVariable UUID answerId,
            @PathVariable int turnOrder,
            @RequestBody UpdateExamItemResponseTurnRequest request) {
        var data = updateExamItemResponseTurnUseCase.execute(new UpdateExamItemResponseTurnCommand(
            answerId,
            turnOrder,
            request.turnType(),
            request.promptText(),
            request.audioUrl(),
            request.transcript(),
            request.durationSeconds(),
            request.wordCount(),
            UpdateExamItemResponseTurnUseCase.parseAnsweredAt(request.answeredAt())
        ));
        return ResponseEntity.ok(ApiResponse.success("Cap nhat turn thanh cong", data));
    }
}
