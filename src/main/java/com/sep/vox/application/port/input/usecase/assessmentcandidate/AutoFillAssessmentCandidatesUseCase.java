package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.AutoFillAssessmentCandidatesCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
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

    public AutoFillAssessmentCandidatesUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentPaperAutoAssigner assessmentPaperAutoAssigner,
            AssessmentScheduleManageAccessService assessmentScheduleManageAccessService,
            AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentPaperAutoAssigner = assessmentPaperAutoAssigner;
        this.assessmentScheduleManageAccessService = assessmentScheduleManageAccessService;
        this.assessmentScheduleCandidateConflictValidator = assessmentScheduleCandidateConflictValidator;
    }

    @Override
    @Transactional
    public List<AssessmentCandidateDto> execute(AutoFillAssessmentCandidatesCommand input) {
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = assessmentScheduleManageAccessService.requireCanManage(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        var requestedIds = input.scheduleIds() == null ? null : new HashSet<>(input.scheduleIds());

        // Tập ca mục tiêu = ca của assessment ∈ {DRAFT, PUBLISHED}, (nếu có scheduleIds thì giao với tập này),
        // sắp theo (startDate, id) tăng dần để khoá theo thứ tự ổn định.
        var targetSchedules = assessmentScheduleRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(schedule -> ASSIGNABLE_STATUSES.contains(schedule.getStatus()))
            .filter(schedule -> requestedIds == null || requestedIds.contains(schedule.getId()))
            .sorted(Comparator
                .comparing((AssessmentSchedule schedule) -> schedule.getStartDate(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(schedule -> schedule.getId()))
            .toList();

        // BƯỚC 1 — Khoá TRƯỚC toàn bộ ca mục tiêu theo thứ tự ổn định, CHƯA đụng candidate.
        // Giữ nguyên bản thể ĐÃ KHOÁ chứ không chỉ id: khung giờ dùng để soát trùng lịch phải là
        // khung đọc dưới FOR UPDATE, không phải bản copy lấy trước khi khoá.
        var lockedSchedules = new ArrayList<AssessmentSchedule>();
        for (var schedule : targetSchedules) {
            var locked = assessmentScheduleRepository.findByIdForUpdate(schedule.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
            lockedSchedules.add(locked);
        }
        if (lockedSchedules.isEmpty()) {
            return List.of();
        }

        // BƯỚC 2 — Chỉ sau khi đã giữ hết lock ca mới lấy candidate chưa gán và rải đều (round-robin).
        var now = Instant.now();
        var unassigned = assessmentCandidateRepository
            .findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(assessment.getId());
        if (unassigned.isEmpty()) {
            return List.of();
        }

        // BƯỚC 3 — Chia nhóm trước, soát trùng lịch từng nhóm theo ĐÚNG khung giờ ca của nhóm đó,
        // rồi mới gán. Mỗi ca một khung giờ khác nhau nên soát cả lượt bằng một khung là sai. Không
        // thể tự đụng nhau trong cùng lượt: unique (assessment_id, student_id) nên mỗi học sinh chỉ xuất
        // hiện một lần trong danh sách chưa gán.
        var groupBySchedule = new LinkedHashMap<UUID, List<AssessmentCandidate>>();
        for (int i = 0; i < unassigned.size(); i++) {
            var target = lockedSchedules.get(i % lockedSchedules.size());
            groupBySchedule.computeIfAbsent(target.getId(), key -> new ArrayList<>()).add(unassigned.get(i));
        }
        for (var target : lockedSchedules) {
            var group = groupBySchedule.get(target.getId());
            if (group != null) {
                assessmentScheduleCandidateConflictValidator.requireCandidatesFree(
                    group, target.getStartDate(), target.getEndDate());
            }
        }

        var assigned = new ArrayList<AssessmentCandidate>();
        int i = 0;
        for (var candidate : unassigned) {
            candidate.assignToSchedule(lockedSchedules.get(i % lockedSchedules.size()).getId(), now, currentUserId);
            assigned.add(candidate);
            i++;
        }
        // Gán đề mặc định cho cả lượt trong một lần: đề của kỳ thi không đổi trong vòng lặp, và rải
        // đều cần nhìn toàn bộ danh sách chứ không gán được từng người một.
        assessmentPaperAutoAssigner.assignPapersIfNeeded(assessment, assigned, now, currentUserId);
        return AssessmentCandidateDto.toDtoList(assessmentCandidateRepository.saveAll(assigned));
    }
}
