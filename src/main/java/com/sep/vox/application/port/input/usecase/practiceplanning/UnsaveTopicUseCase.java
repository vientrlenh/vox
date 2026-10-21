package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UnsaveTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.SavedTopicRepository;

@Service
public class UnsaveTopicUseCase implements IUseCase<UnsaveTopicCommand, Boolean> {

    private final SavedTopicRepository savedTopicRepository;
    private final UserContextPort userContextPort;

    public UnsaveTopicUseCase(
            SavedTopicRepository savedTopicRepository,
            UserContextPort userContextPort) {
        this.savedTopicRepository = savedTopicRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Boolean execute(UnsaveTopicCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        savedTopicRepository.delete(studentId, input.topicId());
        return true;
    }
}
