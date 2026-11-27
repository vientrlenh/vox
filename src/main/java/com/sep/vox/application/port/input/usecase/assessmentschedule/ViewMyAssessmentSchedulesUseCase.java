package com.sep.vox.application.port.input.usecase.assessmentschedule;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.assessmentschedule.ViewAssessmentSchedulesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.mapper.AssessmentScheduleDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;

@Service
@RequiredArgsConstructor
public class ViewMyAssessmentSchedulesUseCase
        implements IUseCase<ViewAssessmentSchedulesQuery, List<AssessmentScheduleDto>> {

    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentRepository assessmentRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentScheduleDto> execute(ViewAssessmentSchedulesQuery input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        List<AssessmentCandidate> candidates = assessmentCandidateRepository.findByStudentId(studentId).stream()
            .filter(candidate -> input.assessmentId() == null || input.assessmentId().equals(candidate.getAssessmentId()))
            .toList();
        if (input.assessmentId() != null && candidates.isEmpty()) {
            throw new ForbiddenException("You are not a candidate of this assessment.");
        }

        Set<UUID> publishedAssessmentIds = publishedAssessmentIdsOf(candidates);

        List<UUID> scheduleIds = candidates.stream()
            .filter(candidate -> publishedAssessmentIds.contains(candidate.getAssessmentId()))
            .map(candidate -> candidate.getScheduleId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        List<AssessmentSchedule> schedules = scheduleIds.isEmpty()
            ? List.<AssessmentSchedule>of()
            : assessmentScheduleRepository.findByIdIn(scheduleIds);
        return AssessmentScheduleDtoMapper.toDtoList(filterAndSortSchedules(schedules, input));
    }

    /**
     * A session only shows when the ASSESSMENT has been published, not only when the session is published.
     *
     * <p>The two statuses are independent: the scheduler can publish a session while the assessment is still
     * DRAFT (SCHEDULE not pressed yet). Previously only the session was filtered, so students saw the schedule of an
     * unpublished assessment -- leaking the timeline before the school finalized it.
     *
     * <p>The same rule as ViewMyAssessmentsUseCase#toRow: only DRAFT is hidden. CANCELLED is still shown so students
     * know the assessment was cancelled, just as a CANCELLED session still passes isVisibleToStudent.
     */
    private Set<UUID> publishedAssessmentIdsOf(List<AssessmentCandidate> candidates) {
        return assessmentRepository.findByIdIn(
                candidates.stream()
                    .map(candidate -> candidate.getAssessmentId())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList()).stream()
            .filter(assessment -> assessment.getStatus() != AssessmentStatus.DRAFT)
            .map(assessment -> assessment.getId())
            .collect(Collectors.toSet());
    }

    private List<AssessmentSchedule> filterAndSortSchedules(List<AssessmentSchedule> schedules, ViewAssessmentSchedulesQuery input) {
        return schedules.stream()
            // The visibility filter comes before the client's filter: a DRAFT session is not published yet -- scheduling is
            // not finished -- so it must not be dug out even when the client passes status=DRAFT.
            .filter(schedule -> schedule.getStatus() != null && schedule.getStatus().isVisibleToStudent())
            .filter(schedule -> input.status() == null || input.status() == schedule.getStatus())
            .filter(schedule -> input.startDate() == null
                || (schedule.getStartDate() != null && !schedule.getStartDate().isBefore(input.startDate())))
            .filter(schedule -> input.endDate() == null
                || (schedule.getStartDate() != null && !schedule.getStartDate().isAfter(input.endDate())))
            .sorted(Comparator.comparing((AssessmentSchedule schedule) -> schedule.getStartDate(),
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }
}
