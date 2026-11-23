package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.SaveTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.SavedTopicRepository;

@Service
@RequiredArgsConstructor
public class SaveTopicUseCase implements IUseCase<SaveTopicCommand, Boolean> {

    private final PracticeTopicRepository practiceTopicRepository;
    private final SavedTopicRepository savedTopicRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Boolean execute(SaveTopicCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        if (!practiceTopicRepository.existsActiveById(input.topicId())) {
            throw new ResourceNotFoundException("Không tìm thấy chủ đề luyện tập.");
        }
        if (!savedTopicRepository.existsForStudent(studentId, input.topicId())) {
            savedTopicRepository.save(studentId, input.topicId());
        }
        return true;
    }
}
