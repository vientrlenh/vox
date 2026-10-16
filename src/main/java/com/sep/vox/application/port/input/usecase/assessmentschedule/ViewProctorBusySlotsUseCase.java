package com.sep.vox.application.port.input.usecase.assessmentschedule;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewProctorBusySlotsQuery;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentschedule.ProctorBusySlotResponse;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;

/**
 * Ai trong danh sách giáo viên đang bận vào đúng khung giờ của một ca thi.
 *
 * <p>Chỉ để giao diện làm mờ sẵn người bận kèm lý do — luật chặn thật nằm ở
 * {@link com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator}, chạy
 * trong transaction lúc ghi. Đây không phải lớp bảo vệ.
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
        var schedule = assessmentScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
        // Cùng luật quyền với danh bạ giám thị: người xem được danh sách giáo viên thì cũng được
        // biết ai trong số đó đang bận.
        assessmentDirectoryAccessService.resolveByAssessmentId(schedule.getAssessmentId());

        return assessmentScheduleProctorRepository.findConflictsForTeachers(
                input.teacherIds(), schedule.getStartDate(), schedule.getEndDate(), schedule.getId()).stream()
            .map(conflict -> new ProctorBusySlotResponse(
                conflict.teacherId(),
                conflict.scheduleId(),
                conflict.startDate() == null ? null : conflict.startDate().toString(),
                conflict.endDate() == null ? null : conflict.endDate().toString()))
            .toList();
    }
}
