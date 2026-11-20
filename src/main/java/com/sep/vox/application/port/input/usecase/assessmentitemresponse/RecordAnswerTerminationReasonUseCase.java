package com.sep.vox.application.port.input.usecase.assessmentitemresponse;

import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;

@Service
public class RecordAnswerTerminationReasonUseCase implements IUseCase<RecordAnswerTerminationReasonUseCase.Command, Void> {

    private final AssessmentItemResponseRepository assessmentItemResponseRepository;

    public RecordAnswerTerminationReasonUseCase(AssessmentItemResponseRepository assessmentItemResponseRepository) {
        this.assessmentItemResponseRepository = assessmentItemResponseRepository;
    }

    @Override
    @Transactional
    public Void execute(Command input) {
        AssessmentItemResponse response = assessmentItemResponseRepository.findById(input.answerId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu trả lời để lưu lý do kết thúc"));
        response.setTerminationReason(input.reason());
        assessmentItemResponseRepository.save(response);
        return null;
    }

    public record Command(UUID answerId, String reason) {
    }
}
