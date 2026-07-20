package com.sep.vox.application.port.input.usecase.stream;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.model.exam.ExamStreamTypePermission;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.IssueStudentStreamTokenCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.StreamTokenProvider;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;

@Service
public class IssueStudentStreamTokenUseCase implements IUseCase<IssueStudentStreamTokenCommand, String> {

    private final UserContextPort userContextPort;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamSessionRepository examSessionRepository;
    private final StreamTokenProvider streamTokenProvider; 


    public IssueStudentStreamTokenUseCase(UserContextPort userContextPort, 
                                            ExamScheduleRepository examScheduleRepository, 
                                            ExamRepository examRepository, 
                                            ExamCandidateRepository examCandidateRepository, 
                                            ExamSessionRepository examSessionRepository, 
                                            StreamTokenProvider streamTokenProvider) {
        this.userContextPort = userContextPort;
        this.examScheduleRepository = examScheduleRepository;
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examSessionRepository = examSessionRepository;
        this.streamTokenProvider = streamTokenProvider;
    }

    @Override
    public String execute(IssueStudentStreamTokenCommand input) {
        var command = normalizeCommand(input);

        var now = OffsetDateTime.now();
        var userId = userContextPort.getCurrentAuthenticatedUserId();

        var examSession = examSessionRepository.findByIdAndInProgress(command.examSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi hoặc phiên thi đã hết hạn"));
        
        var exam = examRepository.findById(examSession.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var streamTypes = resolveStreamTypes(exam, command.streamType());

        var candidate = examCandidateRepository.findById(examSession.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy học viên thi"));
        
        if (!candidate.getStudentId().equals(userId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        
        var schedule = examScheduleRepository.findByIdAndInSchedule(candidate.getScheduleId(), now)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lịch thi"));

        var windowStart = schedule.getStartDate();
        var windowEnd = schedule.getEndDate();

        return streamTokenProvider.generateStreamToken(
            userId.toString(),
            candidate.getId().toString(), 
            schedule.getId().toString(),  
            exam.getId().toString(), 
            examSession.getId().toString(),
            streamTypes, 
            windowStart, 
            windowEnd
        );
    }


    private IssueStudentStreamTokenCommand normalizeCommand(IssueStudentStreamTokenCommand input) {
        return new IssueStudentStreamTokenCommand(
            input.examSessionId(), 
            StringNormalization.trimAndCollapseSpaces(input.streamType())
        );  
    }


    private List<String> resolveStreamTypes(Exam exam, String requestedStreamType) {
        if (exam.getRequiredStreamType() == null) {
            throw new IllegalArgumentException("Kỳ thi không hỗ trợ stream");
        }
        switch (requestedStreamType) {
            case "CAMERA": 
                if (isInvalidRequestedIndividualStreamType(exam, ExamRequiredStreamType.SCREEN)) {
                    throw new IllegalArgumentException("Kỳ thi đang chỉ hỗ trợ stream screen");
                }
                return List.of("camera");
            case "SCREEN": 
                if (isInvalidRequestedIndividualStreamType(exam, ExamRequiredStreamType.CAMERA)) {
                    throw new IllegalArgumentException("Kỳ thi đang chỉ hỗ trợ stream camera");
                }
                return List.of("screen");
            case "CAMERA_AND_SCREEN": 
                if (!exam.getRequiredStreamType().equals(ExamRequiredStreamType.CAMERA_AND_SCREEN)) {
                    throw new IllegalArgumentException("Kỳ thi không yêu cầu đồng thời camera và màn hình");
                }
                return List.of("camera", "screen");
            default: 
                throw new IllegalArgumentException("Loại stream không được hỗ trợ: " + requestedStreamType);
        }
    }

    private boolean isInvalidRequestedIndividualStreamType(Exam exam, ExamRequiredStreamType requiredStreamType) {
        return exam.getRequiredStreamType().equals(requiredStreamType) || (exam.getRequiredStreamType().equals(ExamRequiredStreamType.CAMERA_AND_SCREEN) && (exam.getStreamTypePermission() == null || exam.getStreamTypePermission().equals(ExamStreamTypePermission.ALL)));
    }
}
