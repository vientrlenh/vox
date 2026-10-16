package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentCandidateStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;

@Service
public class UpdateAssessmentCandidateStatusUseCase implements IUseCase<UpdateAssessmentCandidateStatusCommand, AssessmentCandidateDto> {

    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateAssessmentCandidateStatusUseCase(
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentRepository assessmentRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentCandidateDto execute(UpdateAssessmentCandidateStatusCommand input) {
        var candidate = assessmentCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh"));
        var assessment = assessmentRepository.findById(candidate.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi của thí sinh"));
        if (candidate.getScheduleId() == null) {
            throw new IllegalStateException("Thí sinh chưa được xếp ca thi");
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!hasAttendanceAccess(assessment, candidate.getScheduleId(), currentUserId)) {
            throw new ForbiddenException("Bạn không phải giám thị của ca thi này");
        }

        var schedule = assessmentScheduleRepository.findById(candidate.getScheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi của thí sinh"));
        var now = Instant.now();
        var windowStart = schedule.getStartDate().minus(30, ChronoUnit.MINUTES);
        var windowEnd = schedule.getEndDate();
        if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
            throw new IllegalStateException("Chỉ được điểm danh trong khoảng 30 phút trước đến khi kết thúc ca thi");
        }

        candidate.setStatus(input.status());
        candidate.setUpdatedAt(now);
        candidate.setUpdatedBy(currentUserId);
        return AssessmentCandidateDto.toDto(assessmentCandidateRepository.save(candidate));
    }

    private boolean hasAttendanceAccess(Assessment assessment, UUID scheduleId, UUID currentUserId) {
        if (assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, currentUserId)) {
            return true;
        }
        // CHAIR của bài kiểm tra cũng điểm danh được — với bài trên lớp thì giáo viên chủ bài là
        // CHAIR chứ không nhất thiết là giám thị được phân công cho từng ca.
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return true;
        }

        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        return isSchoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId());
    }
}
