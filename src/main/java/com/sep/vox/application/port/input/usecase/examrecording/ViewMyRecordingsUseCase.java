package com.sep.vox.application.port.input.usecase.examrecording;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyRecordingsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamRecordingDto;
import com.sep.vox.domain.mapper.ExamRecordingDtoMapper;
import com.sep.vox.domain.repository.ExamRecordingRepository;

@Service
public class ViewMyRecordingsUseCase implements IUseCase<ViewMyRecordingsQuery, List<ExamRecordingDto>> {

    private final ExamRecordingRepository examRecordingRepository;
    private final UserContextPort userContextPort;

    public ViewMyRecordingsUseCase(
            ExamRecordingRepository examRecordingRepository,
            UserContextPort userContextPort) {
        this.examRecordingRepository = examRecordingRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamRecordingDto> execute(ViewMyRecordingsQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var recordings = examRecordingRepository.findByStudentIdWithAudio(currentUserId);
        return ExamRecordingDtoMapper.toDtoList(recordings);
    }
}
