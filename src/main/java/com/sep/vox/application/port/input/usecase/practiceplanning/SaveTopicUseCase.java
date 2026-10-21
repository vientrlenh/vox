package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.SaveTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.SavedTopicRepository;

@Service
public class SaveTopicUseCase implements IUseCase<SaveTopicCommand, Boolean> {

    private final PracticeTopicRepository practiceTopicRepository;
    private final SavedTopicRepository savedTopicRepository;
    private final UserContextPort userContextPort;

    public SaveTopicUseCase(
            PracticeTopicRepository practiceTopicRepository,
            SavedTopicRepository savedTopicRepository,
            UserContextPort userContextPort) {
        this.practiceTopicRepository = practiceTopicRepository;
        this.savedTopicRepository = savedTopicRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Boolean execute(SaveTopicCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        if (!practiceTopicRepository.existsActiveById(input.topicId())) {
            throw new ResourceNotFoundException("Không tìm thấy chủ đề luyện tập.");
        }
        if (!savedTopicRepository.existsForStudent(studentId, input.topicId())) {
            savedTopicRepository.save(studentId, input.topicId());
        }
        return true;
    }
}
