package com.sep.vox.application.port.input.usecase.examcandidate;

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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AutoFillExamCandidatesCommand;
import com.sep.vox.application.port.input.service.ExamPaperAutoAssigner;
import com.sep.vox.application.port.input.service.ExamScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.ExamScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.service.exam.ExamEditingGuard;

@Service
public class AutoFillExamCandidatesUseCase
        implements IUseCase<AutoFillExamCandidatesCommand, List<ExamCandidateDto>> {

    private static final Set<ExamScheduleStatus> ASSIGNABLE_STATUSES =
        Set.of(ExamScheduleStatus.DRAFT, ExamScheduleStatus.PUBLISHED);

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamPaperAutoAssigner examPaperAutoAssigner;
    private final ExamScheduleManageAccessService examScheduleManageAccessService;
    private final ExamScheduleCandidateConflictValidator examScheduleCandidateConflictValidator;

    public AutoFillExamCandidatesUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamPaperAutoAssigner examPaperAutoAssigner,
            ExamScheduleManageAccessService examScheduleManageAccessService,
            ExamScheduleCandidateConflictValidator examScheduleCandidateConflictValidator) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examPaperAutoAssigner = examPaperAutoAssigner;
        this.examScheduleManageAccessService = examScheduleManageAccessService;
        this.examScheduleCandidateConflictValidator = examScheduleCandidateConflictValidator;
    }

    @Override
    @Transactional
    public List<ExamCandidateDto> execute(AutoFillExamCandidatesCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = examScheduleManageAccessService.requireCanManage(exam);
        ExamEditingGuard.requireScheduleEditable(exam);

        var requestedIds = input.scheduleIds() == null ? null : new HashSet<>(input.scheduleIds());

        // Tập ca mục tiêu = ca của exam ∈ {DRAFT, PUBLISHED}, (nếu có scheduleIds thì giao với tập này),
        // sắp theo (startDate, id) tăng dần để khoá theo thứ tự ổn định.
        var targetSchedules = examScheduleRepository.findByExamId(exam.getId()).stream()
            .filter(schedule -> ASSIGNABLE_STATUSES.contains(schedule.getStatus()))
            .filter(schedule -> requestedIds == null || requestedIds.contains(schedule.getId()))
            .sorted(Comparator
                .comparing((ExamSchedule schedule) -> schedule.getStartDate(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(schedule -> schedule.getId()))
            .toList();

        // BƯỚC 1 — Khoá TRƯỚC toàn bộ ca mục tiêu theo thứ tự ổn định, CHƯA đụng candidate.
        // Giữ nguyên bản thể ĐÃ KHOÁ chứ không chỉ id: khung giờ dùng để soát trùng lịch phải là
        // khung đọc dưới FOR UPDATE, không phải bản copy lấy trước khi khoá.
        var lockedSchedules = new ArrayList<ExamSchedule>();
        for (var schedule : targetSchedules) {
            var locked = examScheduleRepository.findByIdForUpdate(schedule.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
            lockedSchedules.add(locked);
        }
        if (lockedSchedules.isEmpty()) {
            return List.of();
        }

        // BƯỚC 2 — Chỉ sau khi đã giữ hết lock ca mới lấy candidate chưa gán và rải đều (round-robin).
        var now = Instant.now();
        var unassigned = examCandidateRepository
            .findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(exam.getId());
        if (unassigned.isEmpty()) {
            return List.of();
        }

        // BƯỚC 3 — Chia nhóm trước, soát trùng lịch từng nhóm theo ĐÚNG khung giờ ca của nhóm đó,
        // rồi mới gán. Mỗi ca một khung giờ khác nhau nên soát cả lượt bằng một khung là sai. Không
        // thể tự đụng nhau trong cùng lượt: unique (exam_id, student_id) nên mỗi học sinh chỉ xuất
        // hiện một lần trong danh sách chưa gán.
        var groupBySchedule = new LinkedHashMap<UUID, List<ExamCandidate>>();
        for (int i = 0; i < unassigned.size(); i++) {
            var target = lockedSchedules.get(i % lockedSchedules.size());
            groupBySchedule.computeIfAbsent(target.getId(), key -> new ArrayList<>()).add(unassigned.get(i));
        }
        for (var target : lockedSchedules) {
            var group = groupBySchedule.get(target.getId());
            if (group != null) {
                examScheduleCandidateConflictValidator.requireCandidatesFree(
                    group, target.getStartDate(), target.getEndDate());
            }
        }

        var assigned = new ArrayList<ExamCandidate>();
        int i = 0;
        for (var candidate : unassigned) {
            candidate.assignToSchedule(lockedSchedules.get(i % lockedSchedules.size()).getId(), now, currentUserId);
            assigned.add(candidate);
            i++;
        }
        // Gán đề mặc định cho cả lượt trong một lần: đề của kỳ thi không đổi trong vòng lặp, và rải
        // đều cần nhìn toàn bộ danh sách chứ không gán được từng người một.
        examPaperAutoAssigner.assignPapersIfNeeded(exam, assigned, now, currentUserId);
        return ExamCandidateDtoMapper.toDtoList(examCandidateRepository.saveAll(assigned));
    }
}
