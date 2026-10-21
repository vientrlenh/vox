package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.LearnerProfileQueryRepository;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.LearnerProfile;

@Service
public class ViewLearnerProfileUseCase implements IUseCase<Void, LearnerProfile> {

    private final LearnerProfileQueryRepository learnerProfileQueryRepository;
    private final UserContextPort userContextPort;

    public ViewLearnerProfileUseCase(
            LearnerProfileQueryRepository learnerProfileQueryRepository,
            UserContextPort userContextPort) {
        this.learnerProfileQueryRepository = learnerProfileQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    public LearnerProfile execute(Void input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        return LearnerProfile.toResponse(
            learnerProfileQueryRepository.findCurrent(studentId)
        );
    }
}
