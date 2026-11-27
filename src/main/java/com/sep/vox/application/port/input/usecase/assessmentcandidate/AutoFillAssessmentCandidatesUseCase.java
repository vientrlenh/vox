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
import com.sep.vox.application.port.input.command.assessmentcandidate.AutoFillAssessmentCandidatesCommand;
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

        List<AssessmentSchedule> lockedSchedules = lockTargetSchedules(assessment, input);
        if (lockedSchedules.isEmpty()) {
            return List.of();
        }

        // STEP 2 -- Only after every schedule lock is held, fetch the unassigned candidates and spread them (round-robin).
        Instant now = Instant.now();
        List<AssessmentCandidate> unassigned = assessmentCandidateRepository
            .findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(assessment.getId());
        if (unassigned.isEmpty()) {
            return List.of();
        }

        requireGroupsFreeOfConflicts(unassigned, lockedSchedules);
        return assignAndSave(assessment, unassigned, lockedSchedules, now, currentUserId);
    }

    private List<AssessmentCandidateDto> assignAndSave(
        Assessment assessment,
        List<AssessmentCandidate> unassigned,
        List<AssessmentSchedule> lockedSchedules,
        Instant now,
        UUID currentUserId) {
        List<AssessmentCandidate> assigned = assignRoundRobin(unassigned, lockedSchedules, now, currentUserId);
        // The default papers are assigned for the whole batch at once: the papers of the assessment do not change
        // inside the loop, and spreading evenly needs the whole list rather than one person at a time.
        assessmentPaperAutoAssigner.assignPapersIfNeeded(assessment, assigned, now, currentUserId);
        return AssessmentCandidateDtoMapper.toDtoList(assessmentCandidateRepository.saveAll(assigned));
    }

    /**
     * STEP 1 -- Locks ALL target schedules first, in a stable order, before touching any candidate. It keeps the
     * LOCKED instance, not just the id: the time window used for the conflict check must be the one read under
     * FOR UPDATE, not a copy fetched before the lock.
     *
     * <p>Target schedules = the schedules of the assessment in {DRAFT, PUBLISHED} (if scheduleIds is given,
     * intersected with this set), sorted by (startDate, id) ascending so that locking follows a stable order.
     */
    private List<AssessmentSchedule> lockTargetSchedules(
        Assessment assessment, AutoFillAssessmentCandidatesCommand input) {
        Set<UUID> requestedIds = input.scheduleIds() == null ? null : new HashSet<>(input.scheduleIds());
        List<AssessmentSchedule> targetSchedules = assessmentScheduleRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(schedule -> ASSIGNABLE_STATUSES.contains(schedule.getStatus()))
            .filter(schedule -> requestedIds == null || requestedIds.contains(schedule.getId()))
            .sorted(Comparator
                .comparing((AssessmentSchedule schedule) -> schedule.getStartDate(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(schedule -> schedule.getId()))
            .toList();
        List<AssessmentSchedule> lockedSchedules = new ArrayList<AssessmentSchedule>();
        for (AssessmentSchedule schedule : targetSchedules) {
            AssessmentSchedule locked = assessmentScheduleRepository.findByIdForUpdate(schedule.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
            lockedSchedules.add(locked);
        }
        return lockedSchedules;
    }

    /**
     * STEP 3 -- Split into groups first, check schedule conflicts for each group against EXACTLY that group's
     * session time window, and only then assign. Every session has a different time window so checking the whole
     * batch with one window is wrong. They cannot collide with each other within the same batch: unique
     * (assessment_id, student_id) means each student appears only once in the unassigned list.
     */
    private void requireGroupsFreeOfConflicts(
        List<AssessmentCandidate> unassigned, List<AssessmentSchedule> lockedSchedules) {
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
    }

    private List<AssessmentCandidate> assignRoundRobin(
        List<AssessmentCandidate> unassigned, List<AssessmentSchedule> lockedSchedules, Instant now, UUID currentUserId) {
        List<AssessmentCandidate> assigned = new ArrayList<AssessmentCandidate>();
        int i = 0;
        for (AssessmentCandidate candidate : unassigned) {
            candidate.assignToSchedule(lockedSchedules.get(i % lockedSchedules.size()).getId(), now, currentUserId);
            assigned.add(candidate);
            i++;
        }
        return assigned;
    }
}
