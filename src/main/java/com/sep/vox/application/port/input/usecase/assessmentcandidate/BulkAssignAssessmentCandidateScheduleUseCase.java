package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.domain.model.assessment.Assessment;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentcandidate.BulkAssignAssessmentCandidateScheduleCommand;
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

/**
 * Xếp (hoặc gỡ) cả một nhóm thí sinh vào một ca thi trong đúng MỘT transaction.
 *
 * <p>Để giao diện gọi endpoint xếp từng người N lần thì xếp 40 thí sinh mà hỏng ở người thứ 25 sẽ
 * để lại trạng thái dở dang, và mỗi lượt lại khoá đi khoá lại cùng một ca. Ở đây ca chỉ bị khoá một
 * lần, và hỏng thì rollback toàn bộ.
 */
@Service
@RequiredArgsConstructor
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

    @Override
    @Transactional
    public List<AssessmentCandidateDto> execute(BulkAssignAssessmentCandidateScheduleCommand input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        UUID currentUserId = assessmentScheduleManageAccessService.requireCanManage(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        List<UUID> candidateIds = input.candidateIds() == null ? List.<UUID>of() : input.candidateIds();
        if (candidateIds.isEmpty()) {
            return List.of();
        }

        UUID scheduleId = input.scheduleId();
        AssessmentSchedule schedule = scheduleId == null ? null : lockAssignableSchedule(assessment, scheduleId);
        List<AssessmentCandidate> candidates = findAllCandidatesOrFail(assessment, candidateIds);

        requireCandidatesFreeInSchedule(candidates, schedule);

        Instant now = Instant.now();
        List<AssessmentCandidate> assignedToSchedule = applyScheduleChange(candidates, scheduleId, now, currentUserId);
        // Only candidates just placed into a schedule get a paper; when unassigning from a schedule, the paper they
        // already have is kept.
        assessmentPaperAutoAssigner.assignPapersIfNeeded(assessment, assignedToSchedule, now, currentUserId);
        return AssessmentCandidateDtoMapper.toDtoList(assessmentCandidateRepository.saveAll(candidates));
    }

    /**
     * Lock the sessions FIRST, touch the candidates AFTER -- the same order as AutoFillAssessmentCandidatesUseCase
     * so that the two flows running in parallel do not lock each other crosswise.
     */
    private AssessmentSchedule lockAssignableSchedule(Assessment assessment, UUID scheduleId) {
        AssessmentSchedule schedule = assessmentScheduleRepository.findByIdForUpdate(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getAssessmentId().equals(assessment.getId())) {
            throw new ResourceNotFoundException("Không tìm thấy ca thi");
        }
        if (!ASSIGNABLE_STATUSES.contains(schedule.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể xếp thí sinh vào ca ở trạng thái nháp hoặc đã công bố");
        }
        return schedule;
    }

    /**
     * The whole batch is checked with ONE query before anyone is touched: a failure at the 25th person after 24
     * already changed schedule is covered by the rollback, but the message must name everyone so the person
     * scheduling can remove them in one go.
     */
    private void requireCandidatesFreeInSchedule(List<AssessmentCandidate> candidates, AssessmentSchedule schedule) {
        if (schedule != null) {
            assessmentScheduleCandidateConflictValidator.requireCandidatesFree(
                candidates, schedule.getStartDate(), schedule.getEndDate());
        }
    }

    private List<AssessmentCandidate> findAllCandidatesOrFail(Assessment assessment, List<UUID> candidateIds) {
        List<AssessmentCandidate> candidates = assessmentCandidateRepository.findByIdInAndAssessmentId(candidateIds, assessment.getId());
        // Looked up by (id, assessmentId) so a missing row means an id that does not exist or belongs to another assessment --
        // the whole batch fails instead of silently placing only part of it.
        if (candidates.size() != candidateIds.stream().distinct().count()) {
            throw new ResourceNotFoundException("Không tìm thấy thí sinh");
        }
        return candidates;
    }

    private List<AssessmentCandidate> applyScheduleChange(
        List<AssessmentCandidate> candidates, UUID scheduleId, Instant now, UUID currentUserId) {
        List<AssessmentCandidate> assignedToSchedule = new ArrayList<AssessmentCandidate>();
        for (AssessmentCandidate candidate : candidates) {
            if (scheduleId == null) {
                candidate.unassignFromSchedule(now, currentUserId);
                continue;
            }
            candidate.assignToSchedule(scheduleId, now, currentUserId);
            assignedToSchedule.add(candidate);
        }
        return assignedToSchedule;
    }
}
