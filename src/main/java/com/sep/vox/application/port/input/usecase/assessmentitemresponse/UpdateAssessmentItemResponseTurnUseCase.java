package com.sep.vox.application.port.input.usecase.assessmentitemresponse;

import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.assessmentitemresponse.AssessmentItemResponseResponseMapper;
import com.sep.vox.application.port.input.command.assessmentitemresponse.UpdateAssessmentItemResponseTurnCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseTurnResponse;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentItemResponseTurnUseCase
        implements IUseCase<UpdateAssessmentItemResponseTurnCommand, AssessmentItemResponseTurnResponse> {

    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;

    @Override
    @Transactional
    public AssessmentItemResponseTurnResponse execute(UpdateAssessmentItemResponseTurnCommand input) {
        if (!assessmentItemResponseRepository.existsById(input.answerId())) {
            throw new ResourceNotFoundException("Không tìm thấy câu trả lời của thí sinh");
        }

        AssessmentItemResponseTurn turn = assessmentItemResponseTurnRepository.findByAssessmentItemResponseIdAndTurnOrder(input.answerId(), input.turnOrder())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy turn cần cập nhật"));

        if (input.turnType() != null) {
            turn.setTurnType(input.turnType());
        }
        if (input.promptText() != null) {
            turn.setPromptText(input.promptText());
        }
        if (input.audioUrl() != null) {
            turn.setAudioUrl(input.audioUrl());
        }
        if (input.transcript() != null) {
            turn.setTranscript(input.transcript());
        }
        if (input.durationSeconds() != null) {
            turn.setDurationSeconds(input.durationSeconds());
        }
        if (input.wordCount() != null) {
            turn.setWordCount(input.wordCount());
        }
        if (input.answeredAt() != null) {
            turn.setAnsweredAt(input.answeredAt());
        }

        AssessmentItemResponseTurn saved = assessmentItemResponseTurnRepository.upsert(turn);
        return AssessmentItemResponseResponseMapper.toTurnResponse(saved);
    }

}
