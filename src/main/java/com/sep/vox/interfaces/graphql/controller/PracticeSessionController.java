package com.sep.vox.interfaces.graphql.controller;

import static com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;
import static com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.SubmitTurnResult;
import static com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.TeacherPracticeSessionDetail;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.EndPracticeSessionCommand;
import com.sep.vox.application.port.input.command.HeartbeatPracticeSessionCommand;
import com.sep.vox.application.port.input.command.StartPracticeSessionCommand;
import com.sep.vox.application.port.input.command.SubmitPracticeTurnCommand;
import com.sep.vox.application.port.input.query.ViewMyPracticeHistoryQuery;
import com.sep.vox.application.port.input.query.ViewPracticeSessionQuery;
import com.sep.vox.application.port.input.query.ViewStudentPracticeSessionDetailQuery;
import com.sep.vox.application.port.input.usecase.practicesession.EndPracticeSessionUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.HeartbeatPracticeSessionUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.StartPracticeSessionUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.SubmitPracticeTurnUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ViewMyPracticeHistoryUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ViewPracticeSessionUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ViewStudentPracticeSessionDetailUseCase;
import com.sep.vox.domain.model.personalization.SubmitPracticeTurn;
import com.sep.vox.domain.model.personalization.TurnCorrectionSubmission;
import com.sep.vox.interfaces.graphql.dto.request.EndPracticeSessionInput;
import com.sep.vox.interfaces.graphql.dto.request.SubmitPracticeTurnInput;

@Controller
public class PracticeSessionController {

    private final StartPracticeSessionUseCase startPracticeSessionUseCase;
    private final SubmitPracticeTurnUseCase submitPracticeTurnUseCase;
    private final HeartbeatPracticeSessionUseCase heartbeatPracticeSessionUseCase;
    private final EndPracticeSessionUseCase endPracticeSessionUseCase;
    private final ViewPracticeSessionUseCase viewPracticeSessionUseCase;
    private final ViewMyPracticeHistoryUseCase viewMyPracticeHistoryUseCase;
    private final ViewStudentPracticeSessionDetailUseCase viewStudentPracticeSessionDetailUseCase;

    public PracticeSessionController(
            StartPracticeSessionUseCase startPracticeSessionUseCase,
            SubmitPracticeTurnUseCase submitPracticeTurnUseCase,
            HeartbeatPracticeSessionUseCase heartbeatPracticeSessionUseCase,
            EndPracticeSessionUseCase endPracticeSessionUseCase,
            ViewPracticeSessionUseCase viewPracticeSessionUseCase,
            ViewMyPracticeHistoryUseCase viewMyPracticeHistoryUseCase,
            ViewStudentPracticeSessionDetailUseCase viewStudentPracticeSessionDetailUseCase) {
        this.startPracticeSessionUseCase = startPracticeSessionUseCase;
        this.submitPracticeTurnUseCase = submitPracticeTurnUseCase;
        this.heartbeatPracticeSessionUseCase = heartbeatPracticeSessionUseCase;
        this.endPracticeSessionUseCase = endPracticeSessionUseCase;
        this.viewPracticeSessionUseCase = viewPracticeSessionUseCase;
        this.viewMyPracticeHistoryUseCase = viewMyPracticeHistoryUseCase;
        this.viewStudentPracticeSessionDetailUseCase = viewStudentPracticeSessionDetailUseCase;
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public PracticeSession startPracticeSession(@Argument("paperId") UUID paperId) {
        return startPracticeSessionUseCase.execute(new StartPracticeSessionCommand(paperId));
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public SubmitTurnResult submitPracticeTurn(@Argument("input") SubmitPracticeTurnInput input) {
        var corrections = input.corrections() == null
            ? List.<TurnCorrectionSubmission>of()
            : input.corrections().stream()
                .map(correction -> new TurnCorrectionSubmission(
                    correction.category(),
                    correction.originalText(),
                    correction.correctedText(),
                    correction.explanation(),
                    correction.correctAudioUrl(),
                    correction.confidence()
                ))
                .toList();
        var turn = new SubmitPracticeTurn(
            input.sessionId(),
            input.questionId(),
            input.turnOrder(),
            input.turnType(),
            input.promptText(),
            input.audioUrl(),
            input.transcript(),
            input.durationSeconds(),
            input.wordFeedbackJson(),
            input.turnScore(),
            input.questionComplete(),
            corrections
        );
        return submitPracticeTurnUseCase.execute(new SubmitPracticeTurnCommand(turn));
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public boolean heartbeatPracticeSession(@Argument("sessionId") UUID sessionId) {
        return heartbeatPracticeSessionUseCase.execute(new HeartbeatPracticeSessionCommand(sessionId));
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public PracticeSession endPracticeSession(@Argument("input") EndPracticeSessionInput input) {
        return endPracticeSessionUseCase.execute(new EndPracticeSessionCommand(
            input.sessionId(),
            input.helpRequestCount(),
            input.longPauseCount()
        ));
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public PracticeSession practiceSession(@Argument("sessionId") UUID sessionId) {
        return viewPracticeSessionUseCase.execute(new ViewPracticeSessionQuery(sessionId));
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<PracticeSession> myPracticeHistory(@Argument("limit") Integer limit) {
        return viewMyPracticeHistoryUseCase.execute(
            new ViewMyPracticeHistoryQuery(limit == null ? 20 : limit)
        );
    }

    @QueryMapping
    @PreAuthorize("hasRole('TEACHER')")
    public TeacherPracticeSessionDetail studentPracticeSessionDetail(
            @Argument("sessionId") UUID sessionId) {
        return viewStudentPracticeSessionDetailUseCase.execute(
            new ViewStudentPracticeSessionDetailQuery(sessionId)
        );
    }
}
