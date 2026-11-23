package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.mapper.learnerprofile.LearnerProfileResponseMapper;
import com.sep.vox.application.port.input.command.SetPracticeGoalCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.port.input.service.LearnerProfileCommandService;
import com.sep.vox.application.query.repository.LearnerProfileQueryRepository;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.LearnerProfile;

@Service
@RequiredArgsConstructor
public class SetPracticeGoalUseCase implements IUseCase<SetPracticeGoalCommand, LearnerProfile> {

    private final LearnerProfileCommandService commandService;
    private final LearnerProfileQueryRepository queryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public LearnerProfile execute(SetPracticeGoalCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        commandService.setGoal(studentId, input.goalType());
        return LearnerProfileResponseMapper.toResponse(
            queryRepository.findCurrent(studentId)
        );
    }
}
