package com.sep.vox.application.port.input.usecase.recording;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ExamItemResponseDto;
import com.sep.vox.application.query.repository.ExamItemResponseQueryRepository;

@Service
public class ViewMyRecordingsUseCase implements IUseCase<Void, List<ExamItemResponseDto>> {

    private final ExamItemResponseQueryRepository examItemResponseQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyRecordingsUseCase(
            ExamItemResponseQueryRepository examItemResponseQueryRepository,
            UserContextPort userContextPort) {
        this.examItemResponseQueryRepository = examItemResponseQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamItemResponseDto> execute(Void input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        return examItemResponseQueryRepository.findByStudentIdWithAudio(studentId);
    }
}
