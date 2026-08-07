package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.ExamEditingGuard;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AutoFillExamCandidatesCommand;
import com.sep.vox.application.port.input.service.ClassTestPaperAutoAssigner;
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

@Service
public class AutoFillExamCandidatesUseCase
        implements IUseCase<AutoFillExamCandidatesCommand, List<ExamCandidateDto>> {

    private static final Set<ExamScheduleStatus> ASSIGNABLE_STATUSES =
        Set.of(ExamScheduleStatus.DRAFT, ExamScheduleStatus.PUBLISHED);

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ClassTestPaperAutoAssigner classTestPaperAutoAssigner;
    private final ExamScheduleManageAccessService examScheduleManageAccessService;

    public AutoFillExamCandidatesUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamScheduleRepository examScheduleRepository,
            ClassTestPaperAutoAssigner classTestPaperAutoAssigner,
            ExamScheduleManageAccessService examScheduleManageAccessService) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.classTestPaperAutoAssigner = classTestPaperAutoAssigner;
        this.examScheduleManageAccessService = examScheduleManageAccessService;
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
        var lockedScheduleIds = new ArrayList<UUID>();
        for (var schedule : targetSchedules) {
            var locked = examScheduleRepository.findByIdForUpdate(schedule.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
            lockedScheduleIds.add(locked.getId());
        }
        if (lockedScheduleIds.isEmpty()) {
            return List.of();
        }

        // BƯỚC 2 — Chỉ sau khi đã giữ hết lock ca mới lấy candidate chưa gán và rải đều (round-robin).
        var now = Instant.now();
        var unassigned = examCandidateRepository
            .findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(exam.getId());
        if (unassigned.isEmpty()) {
            return List.of();
        }

        // Bài kiểm tra trên lớp chỉ có một đề nên gán luôn, giáo viên không phải bấm thêm bước phân đề.
        // Đề của kỳ thi không đổi trong vòng lặp nên tra đúng MỘT lần ở đây; gọi trong vòng lặp thì mỗi
        // thí sinh là một findByExamId y hệt nhau.
        var singlePaperId = classTestPaperAutoAssigner.resolveSinglePaperId(exam);

        var assigned = new ArrayList<ExamCandidate>();
        int i = 0;
        for (var candidate : unassigned) {
            candidate.assignToSchedule(lockedScheduleIds.get(i % lockedScheduleIds.size()), now, currentUserId);
            if (singlePaperId != null && candidate.getAssignedPaperId() == null) {
                candidate.assignPaper(singlePaperId, now, currentUserId);
            }
            assigned.add(candidate);
            i++;
        }
        return ExamCandidateDtoMapper.toDtoList(examCandidateRepository.saveAll(assigned));
    }
}
