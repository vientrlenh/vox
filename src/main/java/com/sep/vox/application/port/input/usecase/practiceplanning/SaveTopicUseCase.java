package com.sep.vox.application.port.input.usecase.practiceplanning;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SaveTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;
import com.sep.vox.domain.repository.personalization.SavedTopicRepository;

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
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        if (!practiceTopicRepository.existsActiveById(input.topicId())) {
            throw new NotFoundException("Không tìm thấy chủ đề luyện tập.");
        }
        if (!savedTopicRepository.existsForStudent(studentId, input.topicId())) {
            savedTopicRepository.save(studentId, input.topicId());
        }
        return true;
    }
}
