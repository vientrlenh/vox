package com.sep.vox.application.port.input.service;

import com.sep.vox.application.query.dto.SessionRowInfo;
import com.sep.vox.domain.dto.personalization.PracticeCriterionScoreDto;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.TeacherPracticeSessionDetail;
import com.sep.vox.domain.dto.personalization.TeacherPracticeSessionDetailDto;
import com.sep.vox.domain.dto.personalization.TeacherPracticeTurnViewDto;
import com.sep.vox.domain.repository.PracticeCriterionScoreRepository;
import com.sep.vox.domain.repository.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.PracticeResponseTurnRepository;
import com.sep.vox.domain.repository.TurnCorrectionRepository;

/**
 * Builds the detail of a practice session (speaking turns + corrections + score per criterion) WITHOUT checking permission.
 *
 * Split out because there are TWO entry paths to this same content with very different permission rules: a teacher
 * viewing the work of a student they teach ({@code studentPracticeSessionDetail}), and a student reviewing
 * their own work after the session ends ({@code myPracticeSessionDetail}). If each use case
 * built it itself the two copies would drift apart -- adding a column to the student's summary screen
 * and forgetting the teacher side, or vice versa.
 *
 * The permission check is deliberately OUTSIDE this service, in each use case: it does not know who is calling, so
 * calling straight into it and forgetting the permission check exposes another person's work. Read this point carefully before adding
 * a third entry path.
 */
@Service
@RequiredArgsConstructor
public class PracticeSessionDetailAssemblyService {

    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeResponseTurnRepository practiceResponseTurnRepository;
    private final TurnCorrectionRepository turnCorrectionRepository;
    private final PracticeCriterionScoreRepository practiceCriterionScoreRepository;
    private final PracticeItemEvaluationRepository practiceItemEvaluationRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;

    @Transactional(readOnly = true)
    public TeacherPracticeSessionDetail assemble(UUID sessionId) {
        SessionRowInfo summary = practiceSessionQueryRepository.findSessionRowById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Practice session not found."));
        List<TeacherPracticeTurnViewDto> turns = loadTurnViews(sessionId);
        List<PracticeCriterionScoreDto> scores = practiceCriterionScoreRepository.findScoresBySessionId(sessionId);
        int itemCount = practiceItemEvaluationRepository.countCompletedBySessionId(sessionId);
        int duration = summary.getEndedAt() == null
            ? 0
            : (int) Duration.between(summary.getStartedAt(), summary.getEndedAt()).toSeconds();
        return PracticeSessionResponseMapper.toResponse(new TeacherPracticeSessionDetailDto(
            sessionId, summary.getTopicName(), summary.getStartedAt(), duration, itemCount,
            summary.getOverallScore(), scores, "COMPLETED".equals(summary.getStatus()),
            practiceItemResponseRepository.countAwaitingEvaluation(sessionId), countGaveUpAnswers(sessionId),
            practiceItemResponseRepository.findAverageDifficultyRank(sessionId), turns,
            summary.getScoreScaleMin(), summary.getScoreScaleMax()
        ));
    }

    private List<TeacherPracticeTurnViewDto> loadTurnViews(UUID sessionId) {
        return practiceResponseTurnRepository.findBySessionIdOrderByTurnOrder(sessionId).stream()
            .map(turn -> new TeacherPracticeTurnViewDto(
                turn.turnOrder(),
                turn.transcript(),
                turn.audioUrl(),
                turn.wordFeedbackJson(),
                turn.turnScore(),
                turnCorrectionRepository.findByTurnIdOrderById(turn.id())
            ))
            .toList();
    }

    /**
     * Count of answers that were GIVEN UP on. Kept apart from "awaiting" because the two are very
     * different: a waiting answer will get a result eventually, a given-up one never will. Merging
     * them would make the student wait in a loop for a result that will not arrive.
     */
    private int countGaveUpAnswers(UUID sessionId) {
        return practiceItemResponseRepository.countGradingGaveUp(
            sessionId, PracticeGradingFlushService.MAX_GRADING_ATTEMPTS
        );
    }
}
