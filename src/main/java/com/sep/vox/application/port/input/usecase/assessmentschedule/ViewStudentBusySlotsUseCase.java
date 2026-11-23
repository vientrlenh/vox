package com.sep.vox.application.port.input.usecase.assessmentschedule;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.util.ArrayList;
import java.util.List;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewStudentBusySlotsQuery;
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
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
            // Cùng luật quyền với danh bạ học sinh: người xem được danh sách thì cũng được biết ai
            // trong số đó đang bận.
            assessmentDirectoryAccessService.resolveByAssessmentId(schedule.getAssessmentId());

            // Loại chính ca đang xét: học sinh đã ở trong ca này thì không phải "bận" dưới góc nhìn
            // của màn chọn — họ đang ở đúng chỗ.
            assessmentCandidateRepository.findConflictsForStudents(
                    input.studentIds(), schedule.getStartDate(), schedule.getEndDate(), schedule.getId())
                .forEach(conflict -> slots.add(new StudentBusySlotResponse(
                    conflict.studentId(),
                    schedule.getId(),
                    conflict.scheduleId(),
                    conflict.startDate(),
                    conflict.endDate())));
        }
        return slots;
    }
}
