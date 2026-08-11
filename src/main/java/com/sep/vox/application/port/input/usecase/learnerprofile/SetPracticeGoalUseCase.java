package com.sep.vox.application.port.input.usecase.learnerprofile;

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
public class SetPracticeGoalUseCase implements IUseCase<SetPracticeGoalCommand, LearnerProfile> {

    private final LearnerProfileCommandService commandService;
    private final LearnerProfileQueryRepository queryRepository;
    private final UserContextPort userContextPort;

    public SetPracticeGoalUseCase(
            LearnerProfileCommandService commandService,
            LearnerProfileQueryRepository queryRepository,
            UserContextPort userContextPort) {
        this.commandService = commandService;
        this.queryRepository = queryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public LearnerProfile execute(SetPracticeGoalCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        commandService.setGoal(studentId, input.goalType());
        return LearnerProfileResponseMapper.toResponse(
            queryRepository.findCurrent(studentId)
        );
    }
}
