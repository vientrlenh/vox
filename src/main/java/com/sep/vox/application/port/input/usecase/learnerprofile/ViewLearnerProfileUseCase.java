package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.mapper.learnerprofile.LearnerProfileResponseMapper;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.LearnerProfile;
import com.sep.vox.application.query.repository.LearnerProfileQueryRepository;

@Service
@RequiredArgsConstructor
public class ViewLearnerProfileUseCase implements IUseCase<Void, LearnerProfile> {

    private final LearnerProfileQueryRepository learnerProfileQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    public LearnerProfile execute(Void input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        return LearnerProfileResponseMapper.toResponse(
            learnerProfileQueryRepository.findCurrent(studentId)
        );
    }
}
