package com.sep.vox.application.port.input.usecase.examsession;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.ViewExamSessionQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examsession.ExamSessionResponse;

@Service
public class ViewExamSessionUseCase implements IUseCase<ViewExamSessionQuery, ExamSessionResponse> {

    private final ExamResultAccessService examResultAccessService;

    public ViewExamSessionUseCase(ExamResultAccessService examResultAccessService) {
        this.examResultAccessService = examResultAccessService;
    }

    @Override
    public ExamSessionResponse execute(ViewExamSessionQuery input) {
        var session = examResultAccessService.getAuthorizedSession(input.sessionId());
        return CreateExamSessionUseCase.toResponse(session);
    }
}
