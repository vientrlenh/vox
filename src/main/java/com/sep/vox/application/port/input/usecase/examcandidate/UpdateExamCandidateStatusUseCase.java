package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamCandidateStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamCandidateStatusUseCase implements IUseCase<UpdateExamCandidateStatusCommand, ExamCandidateDto> {

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateExamCandidateStatusUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamCandidateDto execute(UpdateExamCandidateStatusCommand input) {
        var candidate = examCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh"));
        var exam = examRepository.findById(candidate.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của thí sinh"));
        if (exam.getKind() != ExamKind.CENTRALIZED) {
            throw new ForbiddenException("Chỉ hỗ trợ điểm danh cho kỳ thi tập trung");
        }
        if (candidate.getScheduleId() == null) {
            throw new IllegalStateException("Thí sinh chưa được xếp ca thi");
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!hasAttendanceAccess(candidate.getScheduleId(), exam.getSchoolId(), currentUserId)) {
            throw new ForbiddenException("Bạn không phải giám thị của ca thi này");
        }

        var schedule = examScheduleRepository.findById(candidate.getScheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi của thí sinh"));
        var now = Instant.now();
        var windowStart = schedule.getStartDate().minus(30, ChronoUnit.MINUTES);
        var windowEnd = schedule.getEndDate();
        if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
            throw new IllegalStateException("Chỉ được điểm danh trong khoảng 30 phút trước đến khi kết thúc ca thi");
        }

        candidate.setStatus(input.status());
        candidate.setUpdatedAt(now);
        candidate.setUpdatedBy(currentUserId);
        return ExamCandidateDtoMapper.toDto(examCandidateRepository.save(candidate));
    }

    private boolean hasAttendanceAccess(UUID scheduleId, UUID examSchoolId, UUID currentUserId) {
        if (examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, currentUserId)) {
            return true;
        }

        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        return isSchoolAdmin && currentSchoolId != null && currentSchoolId.equals(examSchoolId);
    }
}
