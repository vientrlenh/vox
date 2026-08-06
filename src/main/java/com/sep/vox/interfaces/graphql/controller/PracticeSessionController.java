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
import com.sep.vox.application.port.input.command.StartPracticeSessionCommand;
import com.sep.vox.application.port.input.command.SubmitPracticeTurnCommand;
import com.sep.vox.application.port.input.query.ViewMyPracticeHistoryQuery;
import com.sep.vox.application.port.input.query.ViewStudentPracticeSessionDetailQuery;
import com.sep.vox.application.port.input.usecase.practicesession.EndPracticeSessionUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.StartPracticeSessionUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.SubmitPracticeTurnUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ViewMyPracticeHistoryUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ViewMyPracticeSessionDetailUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ViewStudentPracticeSessionDetailUseCase;
import com.sep.vox.domain.model.personalization.SubmitPracticeTurn;
import com.sep.vox.domain.model.personalization.TurnCorrectionSubmission;
import com.sep.vox.interfaces.graphql.dto.request.EndPracticeSessionInput;

@Controller
public class PracticeSessionController {

    private final StartPracticeSessionUseCase startPracticeSessionUseCase;
    private final SubmitPracticeTurnUseCase submitPracticeTurnUseCase;
    private final EndPracticeSessionUseCase endPracticeSessionUseCase;
    private final ViewMyPracticeHistoryUseCase viewMyPracticeHistoryUseCase;
    private final ViewMyPracticeSessionDetailUseCase viewMyPracticeSessionDetailUseCase;
    private final ViewStudentPracticeSessionDetailUseCase viewStudentPracticeSessionDetailUseCase;

    public PracticeSessionController(
            StartPracticeSessionUseCase startPracticeSessionUseCase,
            SubmitPracticeTurnUseCase submitPracticeTurnUseCase,
            EndPracticeSessionUseCase endPracticeSessionUseCase,
            ViewMyPracticeHistoryUseCase viewMyPracticeHistoryUseCase,
            ViewMyPracticeSessionDetailUseCase viewMyPracticeSessionDetailUseCase,
            ViewStudentPracticeSessionDetailUseCase viewStudentPracticeSessionDetailUseCase) {
        this.startPracticeSessionUseCase = startPracticeSessionUseCase;
        this.submitPracticeTurnUseCase = submitPracticeTurnUseCase;
        this.endPracticeSessionUseCase = endPracticeSessionUseCase;
        this.viewMyPracticeHistoryUseCase = viewMyPracticeHistoryUseCase;
        this.viewMyPracticeSessionDetailUseCase = viewMyPracticeSessionDetailUseCase;
        this.viewStudentPracticeSessionDetailUseCase = viewStudentPracticeSessionDetailUseCase;
    }

    @MutationMapping
    @PreAuthorize("hasRole('STUDENT')")
    public PracticeSession startPracticeSession(@Argument("paperId") UUID paperId) {
        return startPracticeSessionUseCase.execute(new StartPracticeSessionCommand(paperId));
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
    public List<PracticeSession> myPracticeHistory(@Argument("limit") Integer limit) {
        return viewMyPracticeHistoryUseCase.execute(
            new ViewMyPracticeHistoryQuery(limit == null ? 20 : limit)
        );
    }

    /**
     * Học sinh xem lại bài của CHÍNH MÌNH (màn tổng kết sau phiên, và xem lại từ lịch sử).
     * Cùng nội dung với {@link #studentPracticeSessionDetail} nhưng khác luật quyền: bên kia
     * là giáo viên xem bài học sinh mình dạy, gọi nhầm sang đó thì học sinh dính Access Denied.
     */
    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public TeacherPracticeSessionDetail myPracticeSessionDetail(
            @Argument("sessionId") UUID sessionId) {
        return viewMyPracticeSessionDetailUseCase.execute(sessionId);
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
