package com.sep.vox.application.port.input.usecase.examsession;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamSessionQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examsession.ExamSessionResponse;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class ViewExamSessionUseCase implements IUseCase<ViewExamSessionQuery, ExamSessionResponse> {

    private final ExamSessionRepository examSessionRepository;

    public ViewExamSessionUseCase(ExamSessionRepository examSessionRepository) {
        this.examSessionRepository = examSessionRepository;
    }

    @Override
    public ExamSessionResponse execute(ViewExamSessionQuery input) {
        var session = examSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        return CreateExamSessionUseCase.toResponse(session);
    }
}
