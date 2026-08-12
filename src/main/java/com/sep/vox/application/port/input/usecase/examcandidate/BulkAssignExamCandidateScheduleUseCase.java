package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BulkAssignExamCandidateScheduleCommand;
import com.sep.vox.application.port.input.service.ClassTestPaperAutoAssigner;
import com.sep.vox.application.port.input.service.ExamScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.service.exam.ExamEditingGuard;

/**
 * Xếp (hoặc gỡ) cả một nhóm thí sinh vào một ca thi trong đúng MỘT transaction.
 *
 * <p>Để giao diện gọi endpoint xếp từng người N lần thì xếp 40 thí sinh mà hỏng ở người thứ 25 sẽ
 * để lại trạng thái dở dang, và mỗi lượt lại khoá đi khoá lại cùng một ca. Ở đây ca chỉ bị khoá một
 * lần, và hỏng thì rollback toàn bộ.
 */
@Service
public class BulkAssignExamCandidateScheduleUseCase
        implements IUseCase<BulkAssignExamCandidateScheduleCommand, List<ExamCandidateDto>> {

    private static final Set<ExamScheduleStatus> ASSIGNABLE_STATUSES =
        Set.of(ExamScheduleStatus.DRAFT, ExamScheduleStatus.PUBLISHED);

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ClassTestPaperAutoAssigner classTestPaperAutoAssigner;
    private final ExamScheduleManageAccessService examScheduleManageAccessService;

    public BulkAssignExamCandidateScheduleUseCase(
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
    public List<ExamCandidateDto> execute(BulkAssignExamCandidateScheduleCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = examScheduleManageAccessService.requireCanManage(exam);
        ExamEditingGuard.requireScheduleEditable(exam);

        var candidateIds = input.candidateIds() == null ? List.<java.util.UUID>of() : input.candidateIds();
        if (candidateIds.isEmpty()) {
            return List.of();
        }

        // Khoá ca TRƯỚC, đụng thí sinh SAU — cùng thứ tự với AutoFillExamCandidatesUseCase để hai
        // luồng chạy song song không khoá chéo nhau.
        var scheduleId = input.scheduleId();
        if (scheduleId != null) {
            var schedule = examScheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
            if (!schedule.getExamId().equals(exam.getId())) {
                throw new NotFoundException("Không tìm thấy ca thi");
            }
            if (!ASSIGNABLE_STATUSES.contains(schedule.getStatus())) {
                throw new IllegalStateException("Chỉ có thể xếp thí sinh vào ca ở trạng thái nháp hoặc đã công bố");
            }
        }

        var candidates = examCandidateRepository.findByIdInAndExamId(candidateIds, exam.getId());
        // Tra theo (id, examId) nên thiếu dòng nghĩa là có id không tồn tại hoặc thuộc kỳ thi khác —
        // hỏng cả lượt thay vì âm thầm xếp một phần.
        if (candidates.size() != candidateIds.stream().distinct().count()) {
            throw new NotFoundException("Không tìm thấy thí sinh");
        }

        var now = Instant.now();
        var singlePaperId = classTestPaperAutoAssigner.resolveSinglePaperId(exam);
        for (var candidate : candidates) {
            if (scheduleId == null) {
                candidate.unassignFromSchedule(now, currentUserId);
                continue;
            }
            candidate.assignToSchedule(scheduleId, now, currentUserId);
            if (singlePaperId != null && candidate.getAssignedPaperId() == null) {
                candidate.assignPaper(singlePaperId, now, currentUserId);
            }
        }
        return ExamCandidateDtoMapper.toDtoList(examCandidateRepository.saveAll(candidates));
    }
}
