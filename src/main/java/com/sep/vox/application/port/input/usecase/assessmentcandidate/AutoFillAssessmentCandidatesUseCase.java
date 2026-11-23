package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.domain.model.assessment.Assessment;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.AutoFillAssessmentCandidatesCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.mapper.AssessmentCandidateDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
@RequiredArgsConstructor
public class AutoFillAssessmentCandidatesUseCase
        implements IUseCase<AutoFillAssessmentCandidatesCommand, List<AssessmentCandidateDto>> {

    private static final Set<AssessmentScheduleStatus> ASSIGNABLE_STATUSES =
        Set.of(AssessmentScheduleStatus.DRAFT, AssessmentScheduleStatus.PUBLISHED);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentPaperAutoAssigner assessmentPaperAutoAssigner;
    private final AssessmentScheduleManageAccessService assessmentScheduleManageAccessService;
    private final AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator;

    @Override
    @Transactional
    public List<AssessmentCandidateDto> execute(AutoFillAssessmentCandidatesCommand input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        UUID currentUserId = assessmentScheduleManageAccessService.requireCanManage(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        Set<UUID> requestedIds = input.scheduleIds() == null ? null : new HashSet<>(input.scheduleIds());

        // Target sessions = the sessions of the assessment in {DRAFT, PUBLISHED}, (if scheduleIds is given, intersected with this set),
        // sorted by (startDate, id) ascending so that locking follows a stable order.
        List<AssessmentSchedule> targetSchedules = assessmentScheduleRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(schedule -> ASSIGNABLE_STATUSES.contains(schedule.getStatus()))
            .filter(schedule -> requestedIds == null || requestedIds.contains(schedule.getId()))
            .sorted(Comparator
                .comparing((AssessmentSchedule schedule) -> schedule.getStartDate(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(schedule -> schedule.getId()))
            .toList();

        // BƯỚC 1 — Khoá TRƯỚC toàn bộ ca mục tiêu theo thứ tự ổn định, CHƯA đụng candidate.
        // Giữ nguyên bản thể ĐÃ KHOÁ chứ không chỉ id: khung giờ dùng để soát trùng lịch phải là
        // khung đọc dưới FOR UPDATE, không phải bản copy lấy trước khi khoá.
        List<AssessmentSchedule> lockedSchedules = new ArrayList<AssessmentSchedule>();
        for (AssessmentSchedule schedule : targetSchedules) {
            AssessmentSchedule locked = assessmentScheduleRepository.findByIdForUpdate(schedule.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
            lockedSchedules.add(locked);
        }
        if (lockedSchedules.isEmpty()) {
            return List.of();
        }

        // BƯỚC 2 — Chỉ sau khi đã giữ hết lock ca mới lấy candidate chưa gán và rải đều (round-robin).
        Instant now = Instant.now();
        List<AssessmentCandidate> unassigned = assessmentCandidateRepository
            .findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(assessment.getId());
        if (unassigned.isEmpty()) {
            return List.of();
        }

        // STEP 3 -- Split into groups first, check schedule conflicts for each group against EXACTLY that group's session time window,
        // and only then assign. Every session has a different time window so checking the whole batch with one window is wrong. They cannot
        // collide with each other within the same batch: unique (assessment_id, student_id) means each student appears
        // only once in the unassigned list.
        Map<UUID, List<AssessmentCandidate>> groupBySchedule = new LinkedHashMap<UUID, List<AssessmentCandidate>>();
        for (int i = 0; i < unassigned.size(); i++) {
            AssessmentSchedule target = lockedSchedules.get(i % lockedSchedules.size());
            groupBySchedule.computeIfAbsent(target.getId(), key -> new ArrayList<>()).add(unassigned.get(i));
        }
        for (AssessmentSchedule target : lockedSchedules) {
            List<AssessmentCandidate> group = groupBySchedule.get(target.getId());
            if (group != null) {
                assessmentScheduleCandidateConflictValidator.requireCandidatesFree(
                    group, target.getStartDate(), target.getEndDate());
            }
        }

        List<AssessmentCandidate> assigned = new ArrayList<AssessmentCandidate>();
        int i = 0;
        for (AssessmentCandidate candidate : unassigned) {
            candidate.assignToSchedule(lockedSchedules.get(i % lockedSchedules.size()).getId(), now, currentUserId);
            assigned.add(candidate);
            i++;
        }
        // Gán đề mặc định cho cả lượt trong một lần: đề của kỳ thi không đổi trong vòng lặp, và rải
        // đều cần nhìn toàn bộ danh sách chứ không gán được từng người một.
        assessmentPaperAutoAssigner.assignPapersIfNeeded(assessment, assigned, now, currentUserId);
        return AssessmentCandidateDtoMapper.toDtoList(assessmentCandidateRepository.saveAll(assigned));
    }
}
