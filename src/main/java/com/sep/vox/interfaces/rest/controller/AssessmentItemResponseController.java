package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseTurnResponse;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.UpdateAssessmentItemResponseTurnCommand;
import com.sep.vox.application.port.input.usecase.assessmentitemresponse.UpdateAssessmentItemResponseTurnUseCase;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentItemResponseTurnRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/assessment-item-responses")
public class AssessmentItemResponseController {

    private final UpdateAssessmentItemResponseTurnUseCase updateAssessmentItemResponseTurnUseCase;

    public AssessmentItemResponseController(
            UpdateAssessmentItemResponseTurnUseCase updateAssessmentItemResponseTurnUseCase) {
        this.updateAssessmentItemResponseTurnUseCase = updateAssessmentItemResponseTurnUseCase;
    }

    @PatchMapping("/{answerId}/turns/{turnOrder}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> updateTurn(
            @PathVariable("answerId") UUID answerId,
            @PathVariable("turnOrder") int turnOrder,
            @RequestBody UpdateAssessmentItemResponseTurnRequest request) {
        AssessmentItemResponseTurnResponse data = updateAssessmentItemResponseTurnUseCase.execute(new UpdateAssessmentItemResponseTurnCommand(
            answerId,
            turnOrder,
            request.turnType(),
            request.promptText(),
            request.audioUrl(),
            request.transcript(),
            request.durationSeconds(),
            request.wordCount(),
            UpdateAssessmentItemResponseTurnUseCase.parseAnsweredAt(request.answeredAt())
        ));
        return ResponseEntity.ok(ApiResponse.success("Cap nhat turn thanh cong", data));
    }
}
