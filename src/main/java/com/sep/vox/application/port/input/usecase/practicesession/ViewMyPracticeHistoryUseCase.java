package com.sep.vox.application.port.input.usecase.practicesession;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.mapper.practicesession.SessionRowMapper;
import com.sep.vox.application.port.input.query.ViewMyPracticeHistoryQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;

@Service
public class ViewMyPracticeHistoryUseCase implements IUseCase<ViewMyPracticeHistoryQuery, List<PracticeSession>> {

    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyPracticeHistoryUseCase(
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            UserContextPort userContextPort) {
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeSession> execute(ViewMyPracticeHistoryQuery input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var limit = Math.max(1, Math.min(input.limit(), 100));
        return practiceSessionQueryRepository.findHistory(studentId, limit).stream()
            .map(SessionRowMapper::toDto)
            .map(PracticeSessionResponseMapper::toResponse)
            .toList();
    }
}
