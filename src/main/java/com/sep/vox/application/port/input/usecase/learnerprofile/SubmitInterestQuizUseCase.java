package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.SubmitInterestQuizCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.LearnerProfileQueryRepository;
import com.sep.vox.application.port.input.service.LearnerProfileCommandService;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.LearnerProfile;

@Service
public class SubmitInterestQuizUseCase implements IUseCase<SubmitInterestQuizCommand, LearnerProfile> {

    private final LearnerProfileCommandService commandService;
    private final LearnerProfileQueryRepository queryRepository;
    private final UserContextPort userContextPort;

    public SubmitInterestQuizUseCase(
            LearnerProfileCommandService commandService,
            LearnerProfileQueryRepository queryRepository,
            UserContextPort userContextPort) {
        this.commandService = commandService;
        this.queryRepository = queryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public LearnerProfile execute(SubmitInterestQuizCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        commandService.submitQuiz(studentId, input.answers());
        return LearnerProfile.toResponse(
            queryRepository.findCurrent(studentId)
        );
    }
}
