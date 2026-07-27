package com.sep.vox.application.port.input.usecase.examschedule;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.ExamScheduleWindowMessages;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamScheduleCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.mapper.ExamScheduleDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class CreateExamScheduleUseCase implements IUseCase<CreateExamScheduleCommand, ExamScheduleDto> {

    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final SchoolRoomRepository schoolRoomRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public CreateExamScheduleUseCase(
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            SchoolRoomRepository schoolRoomRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.schoolRoomRepository = schoolRoomRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamScheduleDto execute(CreateExamScheduleCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(exam);

        if (input.startDate() == null || input.endDate() == null) {
            throw new IllegalArgumentException("Thời gian bắt đầu và kết thúc là bắt buộc");
        }
        if (!input.endDate().isAfter(input.startDate())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (exam.isScheduleWindowShorterThanExamTime(input.startDate(), input.endDate())) {
            throw new IllegalArgumentException(ExamScheduleWindowMessages.tooShortForExamTime(exam));
        }
        if (exam.isScheduleWindowOutsideExamWindow(input.startDate(), input.endDate())) {
            throw new IllegalArgumentException(ExamScheduleWindowMessages.outsideExamWindow(exam));
        }

        SchoolRoom room = schoolRoomRepository.findById(input.schoolRoomId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng học"));
        if (!room.getSchoolId().equals(exam.getSchoolId())) {
            throw new ForbiddenException("Phòng học không thuộc trường của bài kiểm tra");
        }

        if (examScheduleRepository.existsOverlapping(input.schoolRoomId(), input.startDate(), input.endDate(), null)) {
            throw new DuplicatedException("Phòng học đã có ca thi khác trong khoảng thời gian này");
        }

        var schedule = ExamSchedule.createFresh(
            exam.getId(), input.schoolRoomId(), input.startDate(), input.endDate(),
            currentUserId, OffsetDateTime.now());
        return ExamScheduleDtoMapper.toDto(examScheduleRepository.save(schedule));
    }

    private UUID authorize(Exam exam) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId())) {
            return currentUserId;
        }
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            return currentUserId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
