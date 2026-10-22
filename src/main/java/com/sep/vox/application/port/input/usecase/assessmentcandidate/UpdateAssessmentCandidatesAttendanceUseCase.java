package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateAssessmentCandidatesAttendanceCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.assessment.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.school.SchoolUserRole;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;

@Service
public class UpdateAssessmentCandidatesAttendanceUseCase
        implements IUseCase<UpdateAssessmentCandidatesAttendanceCommand, List<AssessmentCandidateDto>> {

    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public UpdateAssessmentCandidatesAttendanceUseCase(
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentRepository assessmentRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<AssessmentCandidateDto> execute(UpdateAssessmentCandidatesAttendanceCommand input) {
        var schedule = assessmentScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
        if (schedule.getStatus() != null && !schedule.getStatus().allowsAttendance()) {
            throw new IllegalStateException("Không thể điểm danh ca thi đã bị huỷ, dời hoặc xoá");
        }
        var candidates = assessmentCandidateRepository.findByScheduleId(schedule.getId());
        if (candidates.isEmpty()) {
            return List.of();
        }

        var assessment = assessmentRepository.findById(schedule.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi của ca thi"));

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        if (!hasAttendanceAccess(assessment, schedule.getId(), currentUserId)) {
            throw new ForbiddenException("Bạn không phải giám thị của ca thi này");
        }

        var now = Instant.now();
        var windowStart = schedule.getStartDate().minus(30, ChronoUnit.MINUTES);
        var windowEnd = schedule.getEndDate();
        if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
            throw new IllegalStateException("Chỉ được điểm danh trong khoảng 30 phút trước giờ bắt đầu đến khi kết thúc ca thi");
        }

        var absentIds = new HashSet<>(input.candidateIds() == null ? List.<UUID>of() : input.candidateIds());
        var scheduleCandidateIds = candidates.stream().map(candidate -> candidate.getId()).collect(Collectors.toSet());
        if (!scheduleCandidateIds.containsAll(absentIds)) {
            throw new IllegalArgumentException("Danh sách thí sinh điểm danh không hợp lệ");
        }

        var changed = new ArrayList<AssessmentCandidate>();
        for (var candidate : candidates) {
            var shouldBeAbsent = absentIds.contains(candidate.getId());
            if (candidate.getStatus() == AssessmentCandidateStatus.EXEMPTED
                    || candidate.getStatus() == AssessmentCandidateStatus.CANCELLED
                    || candidate.getStatus() == AssessmentCandidateStatus.COMPLETED) {
                continue;
            }
            if (shouldBeAbsent && candidate.getStatus() != AssessmentCandidateStatus.ABSENT) {
                candidate.setStatus(AssessmentCandidateStatus.ABSENT);
            } else if (!shouldBeAbsent && candidate.getStatus() != AssessmentCandidateStatus.ATTENDED) {
                candidate.setStatus(AssessmentCandidateStatus.ATTENDED);
            } else {
                continue;
            }
            candidate.setUpdatedAt(now);
            candidate.setUpdatedBy(currentUserId);
            changed.add(candidate);
        }

        if (changed.isEmpty()) {
            return List.of();
        }
        return AssessmentCandidateDto.toDtoList(assessmentCandidateRepository.saveAll(changed));
    }

    private boolean hasAttendanceAccess(Assessment assessment, UUID scheduleId, UUID currentUserId) {
        if (assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, currentUserId)) {
            return true;
        }
        // CHAIR của bài kiểm tra cũng điểm danh được — đồng bộ với mọi chỗ authorize khác của bài
        // trên lớp, nơi giáo viên chủ bài là CHAIR chứ không nhất thiết là giám thị của từng ca.
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return true;
        }

        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var isSchoolAdmin = schoolUserRepository.hasRole(currentUserId, SchoolUserRole.SCHOOL_ADMIN);
        return isSchoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId());
    }
}
