package com.sep.vox.application.port.input.usecase.assessmentschedule;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewProctorBusySlotsQuery;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentschedule.ProctorBusySlotResponse;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;

/**
 * Which teachers in the list are busy during exactly the time window of a session.
 *
 * <p>Only so the UI can grey out busy people ahead of time with a reason -- the real blocking rule lives in
 * {@link com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator}, running
 * inside the transaction at write time. This is not a protection layer.
 */
@Service
public class ViewProctorBusySlotsUseCase
        implements IUseCase<ViewProctorBusySlotsQuery, List<ProctorBusySlotResponse>> {

    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;

    public ViewProctorBusySlotsUseCase(
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            AssessmentDirectoryAccessService assessmentDirectoryAccessService) {
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.assessmentDirectoryAccessService = assessmentDirectoryAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProctorBusySlotResponse> execute(ViewProctorBusySlotsQuery input) {
        if (input.teacherIds() == null || input.teacherIds().isEmpty()) {
            return List.of();
        }
        AssessmentSchedule schedule = assessmentScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        // Same permission rule as the proctor directory: whoever can see the teacher list may also
        // know which of them are busy.
        assessmentDirectoryAccessService.resolveByAssessmentId(schedule.getAssessmentId());

        return assessmentScheduleProctorRepository.findConflictsForTeachers(
                input.teacherIds(), schedule.getStartDate(), schedule.getEndDate(), schedule.getId()).stream()
            .map(conflict -> new ProctorBusySlotResponse(
                conflict.teacherId(),
                conflict.scheduleId(),
                conflict.startDate(),
                conflict.endDate()))
            .toList();
    }
}
