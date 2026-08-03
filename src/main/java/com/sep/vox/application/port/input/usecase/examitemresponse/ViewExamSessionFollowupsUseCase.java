package com.sep.vox.application.port.input.usecase.examitemresponse;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewExamSessionFollowupsQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examitemresponse.ExamSessionFollowupResponse;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;

@Service
public class ViewExamSessionFollowupsUseCase
        implements IUseCase<ViewExamSessionFollowupsQuery, List<ExamSessionFollowupResponse>> {

    private final ExamResultAccessService examResultAccessService;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;

    public ViewExamSessionFollowupsUseCase(
            ExamResultAccessService examResultAccessService,
            ExamItemResponseTurnRepository examItemResponseTurnRepository) {
        this.examResultAccessService = examResultAccessService;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamSessionFollowupResponse> execute(ViewExamSessionFollowupsQuery input) {
        examResultAccessService.requireCandidateVisibleSession(input.sessionId());
        return examItemResponseTurnRepository.countFollowupsBySessionId(input.sessionId()).stream()
            .map(row -> new ExamSessionFollowupResponse(
                row.examItemResponseId(),
                row.followupCount(),
                row.totalTurns()
            ))
            .toList();
    }
}
