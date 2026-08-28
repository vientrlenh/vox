package com.sep.vox.application.port.input.usecase.examitemresponse;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.ExamItemResponseRepository;

@Service
public class RecordAnswerTerminationReasonUseCase implements IUseCase<RecordAnswerTerminationReasonUseCase.Command, Void> {

    private final ExamItemResponseRepository examItemResponseRepository;

    public RecordAnswerTerminationReasonUseCase(ExamItemResponseRepository examItemResponseRepository) {
        this.examItemResponseRepository = examItemResponseRepository;
    }

    @Override
    @Transactional
    public Void execute(Command input) {
        var response = examItemResponseRepository.findById(input.answerId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu trả lời để lưu lý do kết thúc"));
        response.setTerminationReason(input.reason());
        examItemResponseRepository.save(response);
        return null;
    }

    public record Command(UUID answerId, String reason) {
    }
}
