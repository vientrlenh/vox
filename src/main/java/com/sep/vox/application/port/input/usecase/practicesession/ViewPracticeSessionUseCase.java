package com.sep.vox.application.port.input.usecase.practicesession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.mapper.practicesession.SessionRowMapper;
import com.sep.vox.application.port.input.query.ViewPracticeSessionQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;

@Service
public class ViewPracticeSessionUseCase implements IUseCase<ViewPracticeSessionQuery, PracticeSession> {

    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final UserContextPort userContextPort;

    public ViewPracticeSessionUseCase(
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            UserContextPort userContextPort) {
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeSession execute(ViewPracticeSessionQuery input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        return PracticeSessionResponseMapper.toResponse(
            SessionRowMapper.toDto(
                practiceSessionQueryRepository.findSessionRow(input.sessionId(), studentId).orElse(null)
            )
        );
    }
}
