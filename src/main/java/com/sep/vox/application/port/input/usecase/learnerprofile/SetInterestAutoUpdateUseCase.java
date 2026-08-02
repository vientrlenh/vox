package com.sep.vox.application.port.input.usecase.learnerprofile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.mapper.learnerprofile.LearnerProfileResponseMapper;
import com.sep.vox.application.port.input.command.SetInterestAutoUpdateCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.port.input.service.LearnerProfileCommandService;
import com.sep.vox.application.query.repository.LearnerProfileQueryRepository;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.LearnerProfile;

@Service
public class SetInterestAutoUpdateUseCase implements IUseCase<SetInterestAutoUpdateCommand, LearnerProfile> {

    private final LearnerProfileCommandService commandService;
    private final LearnerProfileQueryRepository queryRepository;
    private final UserContextPort userContextPort;

    public SetInterestAutoUpdateUseCase(
            LearnerProfileCommandService commandService,
            LearnerProfileQueryRepository queryRepository,
            UserContextPort userContextPort) {
        this.commandService = commandService;
        this.queryRepository = queryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public LearnerProfile execute(SetInterestAutoUpdateCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        commandService.setAutoUpdate(studentId, input.enabled());
        return LearnerProfileResponseMapper.toResponse(
            queryRepository.findCurrent(studentId)
        );
    }
}
