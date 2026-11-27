package com.sep.vox.application.port.input.usecase.practicesession;

import com.sep.vox.application.port.input.command.practicesession.StartPracticeSessionCommand;
import com.sep.vox.application.port.input.service.StartPracticeSessionPersistenceService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PracticeAgentAvailabilityPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * NOT @Transactional at this layer: agentAvailabilityPort.requireReady() calls out (the Python readiness check,
 * configured timeout up to 12s) -- holding a DB connection open the whole time once exhausted the HikariCP pool
 * under load (the same class of bug as BuildPracticePaperUseCase, fixed in the same round). The real DB writes
 * (read the reserved paper + create the session + change the paper status) live in
 * StartPracticeSessionPersistenceService, a separate transaction that runs AFTER the readiness check finishes.
 */
@Service
@RequiredArgsConstructor
public class StartPracticeSessionUseCase implements IUseCase<StartPracticeSessionCommand, PracticeSession> {

    private final PracticeAgentAvailabilityPort agentAvailabilityPort;
    private final StartPracticeSessionPersistenceService persistenceService;
    private final UserContextPort userContextPort;

    @Override
    public PracticeSession execute(StartPracticeSessionCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        agentAvailabilityPort.requireReady();
        return persistenceService.persist(studentId, input.paperId());
    }
}
