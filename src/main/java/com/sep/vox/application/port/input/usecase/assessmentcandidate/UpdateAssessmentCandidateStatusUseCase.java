package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentCandidateStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.mapper.AssessmentCandidateDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentCandidateStatusUseCase implements IUseCase<UpdateAssessmentCandidateStatusCommand, AssessmentCandidateDto> {

    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentCandidateDto execute(UpdateAssessmentCandidateStatusCommand input) {
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh"));
        Assessment assessment = assessmentRepository.findById(candidate.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của thí sinh"));
        if (candidate.getScheduleId() == null) {
            throw new IllegalStateException("Thí sinh chưa được xếp ca thi");
        }

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!hasAttendanceAccess(assessment, candidate.getScheduleId(), currentUserId)) {
            throw new ForbiddenException("Bạn không phải giám thị của ca thi này");
        }

        AssessmentSchedule schedule = assessmentScheduleRepository.findById(candidate.getScheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi của thí sinh"));
        Instant now = Instant.now();
        Instant windowStart = schedule.getStartDate().minus(30, ChronoUnit.MINUTES);
        Instant windowEnd = schedule.getEndDate();
        if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
            throw new IllegalStateException("Chỉ được điểm danh trong khoảng 30 phút trước đến khi kết thúc ca thi");
        }

        candidate.setStatus(input.status());
        candidate.setUpdatedAt(now);
        candidate.setUpdatedBy(currentUserId);
        return AssessmentCandidateDtoMapper.toDto(assessmentCandidateRepository.save(candidate));
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

        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        return isSchoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId());
    }
}
