package com.sep.vox.application.port.input.usecase.recording;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.GetExamRecordsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamRecordingDto;
import com.sep.vox.domain.mapper.ExamRecordingDtoMapper;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRecordingRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class GetExamRecordsUseCase implements IUseCase<GetExamRecordsQuery, List<ExamRecordingDto>> {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamRecordingRepository examRecordingRepository;
    private final UserContextPort userContextPort;

    public GetExamRecordsUseCase(
        ExamSessionRepository examSessionRepository, 
        ExamRepository examRepository, 
        ExamCandidateRepository examCandidateRepository, 
        ExamMemberRepository examMemberRepository, 
        ExamScheduleProctorRepository examScheduleProctorRepository, 
        ExamRecordingRepository examRecordingRepository, 
        UserContextPort userContextPort
    ) {
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examMemberRepository = examMemberRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examRecordingRepository = examRecordingRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    public List<ExamRecordingDto> execute(GetExamRecordsQuery input) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var schoolId = userContextPort.getCurrentSchoolId();

        var session = examSessionRepository.findById(input.examSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));

        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi"));

        if (schoolId == null || !exam.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Bạn không có quyền xem bản ghi");
            
        }

        if (userContextPort.isSchoolAdmin()) {

        } else if (userContextPort.isTeacher()) {
            var isChair = examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), userId, ExamMemberRole.CHAIR);
            if (!isChair) {
                var candidate = examCandidateRepository.findById(session.getCandidateId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy thi sinh"));

                var scheduleId = candidate.getScheduleId();
                var isProctor = scheduleId != null && examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId);
                if (!isProctor) {
                    throw new ForbiddenException("Bạn không được phân công để giám sát ca thi này");
                }
            }
        } else {
            throw new ForbiddenException("Vai trò hiện tại của bạn không được phép xem bản ghi thi");
        }

        var streamTypeFilter = parseStreamTypeFilter(input.streamType());
        
        var records = examRecordingRepository.findByExamSessionId(session.getId());

        return records.stream()
            .filter(r -> streamTypeFilter == null || r.getStreamType() == streamTypeFilter)
            .map(ExamRecordingDtoMapper::toDto)
            .toList();
    }
    
    private ExamRequiredStreamType parseStreamTypeFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return null;
        }
        ExamRequiredStreamType type;
        try {
            type = ExamRequiredStreamType.valueOf(filter.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại stream không hợp lệ: " + filter);
        }
        if (type ==  ExamRequiredStreamType.CAMERA_AND_SCREEN) {
            throw new IllegalArgumentException("Loại stream yêu cầu cho bản ghi chỉ hỗ trợ màn hình hoặc camera");
        }
        return type;
    }
}
