package com.sep.vox.application.port.input.usecase.assessmentschedule;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.util.ArrayList;
import java.util.List;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.assessmentschedule.ViewStudentBusySlotsQuery;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentschedule.StudentBusySlotResponse;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;

/**
 * Within the group of students under consideration, who is already busy during exactly the time window of each session being considered.
 *
 * <p>Only so the UI can grey out conflicting choices ahead of time with a reason -- the real blocking rule lives in
 * {@link com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator}, running
 * inside the transaction at write time. This is not a protection layer.
 */
@Service
@RequiredArgsConstructor
public class ViewStudentBusySlotsUseCase
        implements IUseCase<ViewStudentBusySlotsQuery, List<StudentBusySlotResponse>> {

    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<StudentBusySlotResponse> execute(ViewStudentBusySlotsQuery input) {
        if (input.studentIds() == null || input.studentIds().isEmpty()
                || input.scheduleIds() == null || input.scheduleIds().isEmpty()) {
            return List.of();
        }

        List<StudentBusySlotResponse> slots = new ArrayList<StudentBusySlotResponse>();
        for (UUID scheduleId : input.scheduleIds()) {
            AssessmentSchedule schedule = assessmentScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment schedule not found"));
            // The same permission rule as the student directory: whoever can see the list may also know who in it is busy.
            assessmentDirectoryAccessService.resolveByAssessmentId(schedule.getAssessmentId());
            addBusySlots(slots, schedule, input.studentIds());
        }
        return slots;
    }

    private void addBusySlots(List<StudentBusySlotResponse> slots, AssessmentSchedule schedule, List<UUID> studentIds) {
        // Exclude the session under consideration itself: a student who is already in this session is not "busy" from the
        // point of view of the picker screen -- they are exactly where they should be.
        assessmentCandidateRepository.findConflictsForStudents(
                studentIds, schedule.getStartDate(), schedule.getEndDate(), schedule.getId())
            .forEach(conflict -> slots.add(new StudentBusySlotResponse(
                conflict.studentId(),
                schedule.getId(),
                conflict.scheduleId(),
                conflict.startDate(),
                conflict.endDate())));
    }
}
