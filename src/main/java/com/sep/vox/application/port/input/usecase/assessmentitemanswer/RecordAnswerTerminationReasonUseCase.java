package com.sep.vox.application.port.input.usecase.assessmentitemanswer;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.assessment.AssessmentItemAnswerRepository;

@Service
public class RecordAnswerTerminationReasonUseCase implements IUseCase<RecordAnswerTerminationReasonUseCase.Command, Void> {

    private final AssessmentItemAnswerRepository assessmentItemAnswerRepository;

    public RecordAnswerTerminationReasonUseCase(AssessmentItemAnswerRepository assessmentItemAnswerRepository) {
        this.assessmentItemAnswerRepository = assessmentItemAnswerRepository;
    }

    @Override
    @Transactional
    public Void execute(Command input) {
        var response = assessmentItemAnswerRepository.findById(input.answerId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu trả lời để lưu lý do kết thúc"));
        response.setTerminationReason(input.reason());
        assessmentItemAnswerRepository.save(response);
        return null;
    }

    public record Command(UUID answerId, String reason) {
    }
}
