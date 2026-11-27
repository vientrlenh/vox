package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentcandidate.UpdateAssessmentCandidatesAttendanceCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.mapper.AssessmentCandidateDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentCandidatesAttendanceUseCase
        implements IUseCase<UpdateAssessmentCandidatesAttendanceCommand, List<AssessmentCandidateDto>> {

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
    public List<AssessmentCandidateDto> execute(UpdateAssessmentCandidatesAttendanceCommand input) {
        AssessmentSchedule schedule = requireAttendableSchedule(input.scheduleId());
        List<AssessmentCandidate> candidates = assessmentCandidateRepository.findByScheduleId(schedule.getId());
        if (candidates.isEmpty()) {
            return List.of();
        }

        Assessment assessment = assessmentRepository.findById(schedule.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi của ca thi"));

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        requireAttendanceAccess(assessment, schedule, currentUserId);

        Instant now = Instant.now();
        requireInsideAttendanceWindow(schedule, now);
        Set<UUID> absentIds = requireValidAbsentIds(input, candidates);

        List<AssessmentCandidate> changed = markAttendance(candidates, absentIds, now, currentUserId);
        if (changed.isEmpty()) {
            return List.of();
        }
        return AssessmentCandidateDtoMapper.toDtoList(assessmentCandidateRepository.saveAll(changed));
    }

    private AssessmentSchedule requireAttendableSchedule(UUID scheduleId) {
        AssessmentSchedule schedule = assessmentScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
        if (schedule.getStatus() != null && !schedule.getStatus().allowsAttendance()) {
            throw new IllegalArgumentException("Không thể điểm danh ca thi đã bị huỷ, dời hoặc xoá");
        }
        return schedule;
    }

    private void requireAttendanceAccess(Assessment assessment, AssessmentSchedule schedule, UUID currentUserId) {
        if (!hasAttendanceAccess(assessment, schedule.getId(), currentUserId)) {
            throw new ForbiddenException("Bạn không phải giám thị của ca thi này");
        }
    }

    private void requireInsideAttendanceWindow(AssessmentSchedule schedule, Instant now) {
        Instant windowStart = schedule.getStartDate().minus(30, ChronoUnit.MINUTES);
        Instant windowEnd = schedule.getEndDate();
        if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
            throw new IllegalArgumentException("Chỉ được điểm danh trong khoảng 30 phút trước giờ bắt đầu đến khi kết thúc ca thi");
        }
    }

    private Set<UUID> requireValidAbsentIds(
        UpdateAssessmentCandidatesAttendanceCommand input, List<AssessmentCandidate> candidates) {
        Set<UUID> absentIds = new HashSet<>(input.candidateIds() == null ? List.<UUID>of() : input.candidateIds());
        Set<UUID> scheduleCandidateIds = candidates.stream().map(candidate -> candidate.getId()).collect(Collectors.toSet());
        if (!scheduleCandidateIds.containsAll(absentIds)) {
            throw new IllegalArgumentException("Danh sách thí sinh điểm danh không hợp lệ");
        }
        return absentIds;
    }

    private List<AssessmentCandidate> markAttendance(
        List<AssessmentCandidate> candidates, Set<UUID> absentIds, Instant now, UUID currentUserId) {
        List<AssessmentCandidate> changed = new ArrayList<AssessmentCandidate>();
        for (AssessmentCandidate candidate : candidates) {
            boolean shouldBeAbsent = absentIds.contains(candidate.getId());
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
        return changed;
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

        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        return isSchoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId());
    }
}
