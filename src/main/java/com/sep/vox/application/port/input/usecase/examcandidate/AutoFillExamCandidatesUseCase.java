package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AutoFillExamCandidatesCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class AutoFillExamCandidatesUseCase
        implements IUseCase<AutoFillExamCandidatesCommand, List<ExamCandidateDto>> {

    private static final Set<ExamScheduleStatus> ASSIGNABLE_STATUSES =
        Set.of(ExamScheduleStatus.DRAFT, ExamScheduleStatus.PUBLISHED);

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final SchoolRoomRepository schoolRoomRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public AutoFillExamCandidatesUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamScheduleRepository examScheduleRepository,
            SchoolRoomRepository schoolRoomRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.schoolRoomRepository = schoolRoomRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<ExamCandidateDto> execute(AutoFillExamCandidatesCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(exam);

        var requestedIds = input.scheduleIds() == null ? null : new HashSet<>(input.scheduleIds());

        // Tập ca mục tiêu = ca của exam ∈ {DRAFT, PUBLISHED}, (nếu có scheduleIds thì giao với tập này),
        // sắp theo (startDate, id) tăng dần để khoá theo thứ tự ổn định.
        var targetSchedules = examScheduleRepository.findByExamId(exam.getId()).stream()
            .filter(schedule -> ASSIGNABLE_STATUSES.contains(schedule.getStatus()))
            .filter(schedule -> requestedIds == null || requestedIds.contains(schedule.getId()))
            .sorted(Comparator
                .comparing(ExamSchedule::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ExamSchedule::getId))
            .toList();

        // BƯỚC 1 — Khoá TRƯỚC toàn bộ ca mục tiêu và tính sẵn sức chứa còn trống, CHƯA đụng candidate.
        var slots = new ArrayList<CapacitySlot>();
        for (var schedule : targetSchedules) {
            var locked = examScheduleRepository.findByIdForUpdate(schedule.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
            var room = schoolRoomRepository.findById(locked.getSchoolRoomId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng học"));
            long remaining;
            if (room.getCapacity() == null) {
                remaining = Long.MAX_VALUE;
            } else {
                remaining = room.getCapacity() - examCandidateRepository.countByScheduleId(locked.getId());
            }
            if (remaining > 0) {
                slots.add(new CapacitySlot(locked.getId(), remaining));
            }
        }

        // BƯỚC 2 — Chỉ sau khi đã giữ hết lock ca mới lấy candidate chưa gán và rải tuần tự.
        var now = OffsetDateTime.now();
        var unassigned = examCandidateRepository
            .findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(exam.getId());
        var assigned = new ArrayList<ExamCandidate>();
        int slotIndex = 0;
        for (var candidate : unassigned) {
            while (slotIndex < slots.size() && slots.get(slotIndex).remaining <= 0) {
                slotIndex++;
            }
            if (slotIndex >= slots.size()) {
                break;
            }
            var slot = slots.get(slotIndex);
            candidate.assignToSchedule(slot.scheduleId, now, currentUserId);
            slot.remaining--;
            assigned.add(candidate);
        }

        if (assigned.isEmpty()) {
            return List.of();
        }
        return ExamCandidateDtoMapper.toDtoList(examCandidateRepository.saveAll(assigned));
    }

    private static final class CapacitySlot {
        private final UUID scheduleId;
        private long remaining;

        private CapacitySlot(UUID scheduleId, long remaining) {
            this.scheduleId = scheduleId;
            this.remaining = remaining;
        }
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
