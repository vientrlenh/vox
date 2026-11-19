package com.sep.vox.application.port.input.usecase.examcandidate;

import com.sep.vox.domain.model.exam.ExamSchedule;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamCandidatesAttendanceCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamCandidatesAttendanceUseCase
        implements IUseCase<UpdateExamCandidatesAttendanceCommand, List<ExamCandidateDto>> {

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateExamCandidatesAttendanceUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examMemberRepository = examMemberRepository;
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
    public List<ExamCandidateDto> execute(UpdateExamCandidatesAttendanceCommand input) {
        ExamSchedule schedule = examScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        if (schedule.getStatus() != null && !schedule.getStatus().allowsAttendance()) {
            throw new IllegalStateException("Không thể điểm danh ca thi đã bị huỷ, dời hoặc xoá");
        }
        List<ExamCandidate> candidates = examCandidateRepository.findByScheduleId(schedule.getId());
        if (candidates.isEmpty()) {
            return List.of();
        }

        Exam exam = examRepository.findById(schedule.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của ca thi"));

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!hasAttendanceAccess(exam, schedule.getId(), currentUserId)) {
            throw new ForbiddenException("Bạn không phải giám thị của ca thi này");
        }

        Instant now = Instant.now();
        Instant windowStart = schedule.getStartDate().minus(30, ChronoUnit.MINUTES);
        Instant windowEnd = schedule.getEndDate();
        if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
            throw new IllegalStateException("Chỉ được điểm danh trong khoảng 30 phút trước giờ bắt đầu đến khi kết thúc ca thi");
        }

        Set<UUID> absentIds = new HashSet<>(input.candidateIds() == null ? List.<UUID>of() : input.candidateIds());
        Set<UUID> scheduleCandidateIds = candidates.stream().map(candidate -> candidate.getId()).collect(Collectors.toSet());
        if (!scheduleCandidateIds.containsAll(absentIds)) {
            throw new IllegalArgumentException("Danh sách thí sinh điểm danh không hợp lệ");
        }

        List<ExamCandidate> changed = new ArrayList<ExamCandidate>();
        for (ExamCandidate candidate : candidates) {
            boolean shouldBeAbsent = absentIds.contains(candidate.getId());
            if (candidate.getStatus() == ExamCandidateStatus.EXEMPTED
                    || candidate.getStatus() == ExamCandidateStatus.CANCELLED
                    || candidate.getStatus() == ExamCandidateStatus.COMPLETED) {
                continue;
            }
            if (shouldBeAbsent && candidate.getStatus() != ExamCandidateStatus.ABSENT) {
                candidate.setStatus(ExamCandidateStatus.ABSENT);
            } else if (!shouldBeAbsent && candidate.getStatus() != ExamCandidateStatus.ATTENDED) {
                candidate.setStatus(ExamCandidateStatus.ATTENDED);
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
        return ExamCandidateDtoMapper.toDtoList(examCandidateRepository.saveAll(changed));
    }

    private boolean hasAttendanceAccess(Exam exam, UUID scheduleId, UUID currentUserId) {
        if (examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, currentUserId)) {
            return true;
        }
        // CHAIR của bài kiểm tra cũng điểm danh được — đồng bộ với mọi chỗ authorize khác của bài
        // trên lớp, nơi giáo viên chủ bài là CHAIR chứ không nhất thiết là giám thị của từng ca.
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            return true;
        }

        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        return isSchoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId());
    }
}
