package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.ExamEditingGuard;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AssignExamCandidateScheduleCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class AssignExamCandidateScheduleUseCase
        implements IUseCase<AssignExamCandidateScheduleCommand, ExamCandidateDto> {

    private static final Set<ExamScheduleStatus> ASSIGNABLE_STATUSES =
        Set.of(ExamScheduleStatus.DRAFT, ExamScheduleStatus.PUBLISHED);

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public AssignExamCandidateScheduleUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamCandidateDto execute(AssignExamCandidateScheduleCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(exam);
        ExamEditingGuard.requireScheduleEditable(exam);

        var candidate = examCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh"));
        if (!candidate.getExamId().equals(exam.getId())) {
            throw new NotFoundException("Không tìm thấy thí sinh");
        }

        var now = Instant.now();

        // Bỏ gán khỏi ca luôn được phép, kể cả khi thí sinh đã không có ca (no-op).
        if (input.scheduleId() == null) {
            candidate.unassignFromSchedule(now, currentUserId);
            return ExamCandidateDtoMapper.toDto(examCandidateRepository.save(candidate));
        }

        // Gán lại đúng ca hiện tại: short-circuit, không kiểm tra lại sức chứa (tránh tự-đếm-chính-mình).
        if (input.scheduleId().equals(candidate.getScheduleId())) {
            return ExamCandidateDtoMapper.toDto(candidate);
        }

        var schedule = examScheduleRepository.findByIdForUpdate(input.scheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getExamId().equals(exam.getId())) {
            throw new NotFoundException("Không tìm thấy ca thi");
        }
        if (!ASSIGNABLE_STATUSES.contains(schedule.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xếp thí sinh vào ca ở trạng thái nháp hoặc đã công bố");
        }

        candidate.assignToSchedule(schedule.getId(), now, currentUserId);
        return ExamCandidateDtoMapper.toDto(examCandidateRepository.save(candidate));
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
