package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.HeartbeatPracticeSessionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;

@Service
public class HeartbeatPracticeSessionUseCase implements IUseCase<HeartbeatPracticeSessionCommand, Boolean> {

    private final PracticeSessionRepository practiceSessionRepository;
    private final UserContextPort userContextPort;

    public HeartbeatPracticeSessionUseCase(
            PracticeSessionRepository practiceSessionRepository,
            UserContextPort userContextPort) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Boolean execute(HeartbeatPracticeSessionCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var session = practiceSessionRepository
            .findByIdAndStudentId(input.sessionId(), studentId)
            .orElse(null);
        if (session == null || !"IN_PROGRESS".equals(session.status())) {
            return false;
        }
        practiceSessionRepository.save(session.withLastHeartbeatAt(OffsetDateTime.now()));
        return true;
    }
}
