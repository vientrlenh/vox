package com.sep.vox.application.port.input.usecase.examitemresponse;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamSessionFollowupsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examitemresponse.ExamSessionFollowupResponse;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class ViewExamSessionFollowupsUseCase
        implements IUseCase<ViewExamSessionFollowupsQuery, List<ExamSessionFollowupResponse>> {

    private final ExamSessionRepository examSessionRepository;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;

    public ViewExamSessionFollowupsUseCase(
            ExamSessionRepository examSessionRepository,
            ExamItemResponseTurnRepository examItemResponseTurnRepository) {
        this.examSessionRepository = examSessionRepository;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamSessionFollowupResponse> execute(ViewExamSessionFollowupsQuery input) {
        if (!examSessionRepository.existsById(input.sessionId())) {
            throw new NotFoundException("Không tìm thấy phiên thi");
        }

        return examItemResponseTurnRepository.countFollowupsBySessionId(input.sessionId()).stream()
            .map(row -> new ExamSessionFollowupResponse(
                row.examItemResponseId(),
                row.followupCount(),
                row.totalTurns()
            ))
            .toList();
    }
}
