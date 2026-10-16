package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.BulkAssignAssessmentCandidateScheduleCommand;
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

/**
 * Xếp (hoặc gỡ) cả một nhóm thí sinh vào một ca thi trong đúng MỘT transaction.
 *
 * <p>Để giao diện gọi endpoint xếp từng người N lần thì xếp 40 thí sinh mà hỏng ở người thứ 25 sẽ
 * để lại trạng thái dở dang, và mỗi lượt lại khoá đi khoá lại cùng một ca. Ở đây ca chỉ bị khoá một
 * lần, và hỏng thì rollback toàn bộ.
 */
@Service
public class BulkAssignAssessmentCandidateScheduleUseCase
        implements IUseCase<BulkAssignAssessmentCandidateScheduleCommand, List<AssessmentCandidateDto>> {

    private static final Set<AssessmentScheduleStatus> ASSIGNABLE_STATUSES =
        Set.of(AssessmentScheduleStatus.DRAFT, AssessmentScheduleStatus.PUBLISHED);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentPaperAutoAssigner assessmentPaperAutoAssigner;
    private final AssessmentScheduleManageAccessService assessmentScheduleManageAccessService;
    private final AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator;

    public BulkAssignAssessmentCandidateScheduleUseCase(
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
    public List<AssessmentCandidateDto> execute(BulkAssignAssessmentCandidateScheduleCommand input) {
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = assessmentScheduleManageAccessService.requireCanManage(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        var candidateIds = input.candidateIds() == null ? List.<java.util.UUID>of() : input.candidateIds();
        if (candidateIds.isEmpty()) {
            return List.of();
        }

        // Khoá ca TRƯỚC, đụng thí sinh SAU — cùng thứ tự với AutoFillAssessmentCandidatesUseCase để hai
        // luồng chạy song song không khoá chéo nhau.
        var scheduleId = input.scheduleId();
        AssessmentSchedule schedule = null;
        if (scheduleId != null) {
            schedule = assessmentScheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
            if (!schedule.getAssessmentId().equals(assessment.getId())) {
                throw new ResourceNotFoundException("Không tìm thấy ca thi");
            }
            if (!ASSIGNABLE_STATUSES.contains(schedule.getStatus())) {
                throw new IllegalStateException("Chỉ có thể xếp thí sinh vào ca ở trạng thái nháp hoặc đã công bố");
            }
        }

        var candidates = assessmentCandidateRepository.findByIdInAndAssessmentId(candidateIds, assessment.getId());
        // Tra theo (id, assessmentId) nên thiếu dòng nghĩa là có id không tồn tại hoặc thuộc kỳ thi khác —
        // hỏng cả lượt thay vì âm thầm xếp một phần.
        if (candidates.size() != candidateIds.stream().distinct().count()) {
            throw new ResourceNotFoundException("Không tìm thấy thí sinh");
        }

        // Soát cả lô bằng MỘT query trước khi đụng vào ai: hỏng ở người thứ 25 mà 24 người trước đã
        // đổi ca thì rollback lo được, nhưng message phải nêu đủ tên để người xếp lịch gỡ một lần.
        if (schedule != null) {
            assessmentScheduleCandidateConflictValidator.requireCandidatesFree(
                candidates, schedule.getStartDate(), schedule.getEndDate());
        }

        var now = Instant.now();
        var assignedToSchedule = new ArrayList<AssessmentCandidate>();
        for (var candidate : candidates) {
            if (scheduleId == null) {
                candidate.unassignFromSchedule(now, currentUserId);
                continue;
            }
            candidate.assignToSchedule(scheduleId, now, currentUserId);
            assignedToSchedule.add(candidate);
        }
        // Chỉ thí sinh vừa được xếp vào ca mới được gán đề; bỏ gán khỏi ca thì giữ nguyên đề đang có.
        assessmentPaperAutoAssigner.assignPapersIfNeeded(assessment, assignedToSchedule, now, currentUserId);
        return AssessmentCandidateDto.toDtoList(assessmentCandidateRepository.saveAll(candidates));
    }
}
