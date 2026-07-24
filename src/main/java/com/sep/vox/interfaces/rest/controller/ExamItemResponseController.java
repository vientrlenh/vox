package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.UpdateExamItemResponseTurnCommand;
import com.sep.vox.application.port.input.usecase.examitemresponse.UpdateExamItemResponseTurnUseCase;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamItemResponseTurnRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/exam-item-responses")
public class ExamItemResponseController {

    private final UpdateExamItemResponseTurnUseCase updateExamItemResponseTurnUseCase;

    public ExamItemResponseController(
            UpdateExamItemResponseTurnUseCase updateExamItemResponseTurnUseCase) {
        this.updateExamItemResponseTurnUseCase = updateExamItemResponseTurnUseCase;
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
