package com.sep.vox.application.port.input.usecase.examschedule;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewProctorBusySlotsQuery;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examschedule.ProctorBusySlotResponse;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;

/**
 * Ai trong danh sách giáo viên đang bận vào đúng khung giờ của một ca thi.
 *
 * <p>Chỉ để giao diện làm mờ sẵn người bận kèm lý do — luật chặn thật nằm ở
 * {@link com.sep.vox.application.port.input.service.ExamScheduleProctorConflictValidator}, chạy
 * trong transaction lúc ghi. Đây không phải lớp bảo vệ.
 */
@Service
public class ViewProctorBusySlotsUseCase
        implements IUseCase<ViewProctorBusySlotsQuery, List<ProctorBusySlotResponse>> {

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamDirectoryAccessService examDirectoryAccessService;

    public ViewProctorBusySlotsUseCase(
            ExamScheduleRepository examScheduleRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamDirectoryAccessService examDirectoryAccessService) {
        this.examScheduleRepository = examScheduleRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examDirectoryAccessService = examDirectoryAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProctorBusySlotResponse> execute(ViewProctorBusySlotsQuery input) {
        if (input.teacherIds() == null || input.teacherIds().isEmpty()) {
            return List.of();
        }
        var schedule = examScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        // Cùng luật quyền với danh bạ giám thị: người xem được danh sách giáo viên thì cũng được
        // biết ai trong số đó đang bận.
        examDirectoryAccessService.resolveByExamId(schedule.getExamId());

        return examScheduleProctorRepository.findConflictsForTeachers(
                input.teacherIds(), schedule.getStartDate(), schedule.getEndDate(), schedule.getId()).stream()
            .map(conflict -> new ProctorBusySlotResponse(
                conflict.teacherId(),
                conflict.scheduleId(),
                conflict.startDate() == null ? null : conflict.startDate().toString(),
                conflict.endDate() == null ? null : conflict.endDate().toString()))
            .toList();
    }
}
