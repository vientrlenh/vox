package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.port.input.query.ViewStudentPracticeSessionDetailQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.TeacherPracticeSessionDetail;
import com.sep.vox.domain.dto.personalization.TeacherPracticeSessionDetailDto;
import com.sep.vox.domain.dto.personalization.TeacherPracticeTurnViewDto;
import com.sep.vox.domain.repository.personalization.PracticeCriterionScoreRepository;
import com.sep.vox.domain.repository.personalization.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.personalization.PracticeResponseTurnRepository;
import com.sep.vox.domain.repository.personalization.TurnCorrectionRepository;

@Service
public class ViewStudentPracticeSessionDetailUseCase
        implements IUseCase<ViewStudentPracticeSessionDetailQuery, TeacherPracticeSessionDetail> {

    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeResponseTurnRepository practiceResponseTurnRepository;
    private final TurnCorrectionRepository turnCorrectionRepository;
    private final PracticeCriterionScoreRepository practiceCriterionScoreRepository;
    private final PracticeItemEvaluationRepository practiceItemEvaluationRepository;
    private final UserContextPort userContextPort;

    public ViewStudentPracticeSessionDetailUseCase(
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            PracticeResponseTurnRepository practiceResponseTurnRepository,
            TurnCorrectionRepository turnCorrectionRepository,
            PracticeCriterionScoreRepository practiceCriterionScoreRepository,
            PracticeItemEvaluationRepository practiceItemEvaluationRepository,
            UserContextPort userContextPort) {
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.practiceResponseTurnRepository = practiceResponseTurnRepository;
        this.turnCorrectionRepository = turnCorrectionRepository;
        this.practiceCriterionScoreRepository = practiceCriterionScoreRepository;
        this.practiceItemEvaluationRepository = practiceItemEvaluationRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherPracticeSessionDetail execute(ViewStudentPracticeSessionDetailQuery input) {
        var teacherId = userContextPort.getCurrentAuthenticatedUserId();
        var sessionId = input.sessionId();
        if (!practiceSessionQueryRepository.canTeacherReadSession(teacherId, sessionId)) {
            throw new ForbiddenException("Bạn không được xem phiên luyện này.");
        }
        var summary = practiceSessionQueryRepository.findSessionRowById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
        var turns = practiceResponseTurnRepository.findBySessionIdOrderByTurnOrder(sessionId).stream()
            .map(turn -> new TeacherPracticeTurnViewDto(
                turn.turnOrder(),
                turn.transcript(),
                turn.audioUrl(),
                turn.wordFeedbackJson(),
                turn.turnScore(),
                turnCorrectionRepository.findByTurnIdOrderById(turn.id())
            ))
            .toList();
        var scores = practiceCriterionScoreRepository.findScoresBySessionId(sessionId);
        var itemCount = practiceItemEvaluationRepository.countCompletedBySessionId(sessionId);
        var duration = summary.getEndedAt() == null
            ? 0
            : (int) Duration.between(summary.getStartedAt(), summary.getEndedAt()).toSeconds();
        return PracticeSessionResponseMapper.toResponse(new TeacherPracticeSessionDetailDto(
            sessionId,
            summary.getTopicName(),
            summary.getStartedAt().toString(),
            duration,
            itemCount,
            summary.getOverallScore(),
            scores,
            "COMPLETED".equals(summary.getStatus()),
            turns
        ));
    }
}
