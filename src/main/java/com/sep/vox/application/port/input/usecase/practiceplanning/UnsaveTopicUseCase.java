package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.practiceplanning.UnsaveTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.SavedTopicRepository;

@Service
@RequiredArgsConstructor
public class UnsaveTopicUseCase implements IUseCase<UnsaveTopicCommand, Boolean> {

    private final SavedTopicRepository savedTopicRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Boolean execute(UnsaveTopicCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        savedTopicRepository.delete(studentId, input.topicId());
        return true;
    }
}
