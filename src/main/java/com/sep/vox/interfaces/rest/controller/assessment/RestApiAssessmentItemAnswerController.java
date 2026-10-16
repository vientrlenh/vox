package com.sep.vox.interfaces.rest.controller.assessment;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.UpdateAssessmentItemAnswerTurnCommand;
import com.sep.vox.application.port.input.usecase.assessmentitemanswer.UpdateAssessmentItemAnswerTurnUseCase;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentItemAnswerTurnRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/assessment-item-answers")
@RequiredArgsConstructor 
public class RestApiAssessmentItemAnswerController {

    private final UpdateAssessmentItemAnswerTurnUseCase updateAssessmentItemAnswerTurnUseCase;

    @PatchMapping("/{answerId}/turns/{turnOrder}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<?>> updateTurn(
            @PathVariable("answerId") UUID answerId,
            @PathVariable("turnOrder") int turnOrder,
            @RequestBody UpdateAssessmentItemAnswerTurnRequest request) {
        var data = updateAssessmentItemAnswerTurnUseCase.execute(new UpdateAssessmentItemAnswerTurnCommand(
            answerId,
            turnOrder,
            request.turnType(),
            request.promptText(),
            request.audioUrl(),
            request.transcript(),
            request.durationSeconds(),
            request.wordCount(),
            UpdateAssessmentItemAnswerTurnUseCase.parseAnsweredAt(request.answeredAt())
        ));
        return ResponseEntity.ok(ApiResponse.success("Cap nhat turn thanh cong", data));
    }
}
