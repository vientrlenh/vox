package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseTurnResponse;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.assessmentitemresponse.UpdateAssessmentItemResponseTurnCommand;
import com.sep.vox.application.port.input.usecase.assessmentitemresponse.UpdateAssessmentItemResponseTurnUseCase;
import com.sep.vox.interfaces.rest.dto.request.assessmentitemresponse.UpdateAssessmentItemResponseTurnRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/assessment-item-responses")
@RequiredArgsConstructor
public class RestAssessmentItemResponseController {

    private final UpdateAssessmentItemResponseTurnUseCase updateAssessmentItemResponseTurnUseCase;

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
            request.answeredAt()
        ));
        return ResponseEntity.ok(ApiResponse.success("Turn updated successfully", data));
    }
}
