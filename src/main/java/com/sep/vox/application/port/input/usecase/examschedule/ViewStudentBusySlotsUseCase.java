package com.sep.vox.application.port.input.usecase.examschedule;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewStudentBusySlotsQuery;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examschedule.StudentBusySlotResponse;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;

/**
 * Trong nhóm học sinh đang xét, ai đã bận vào đúng khung giờ của từng ca thi đang cân nhắc.
 *
 * <p>Chỉ để giao diện làm mờ sẵn lựa chọn bị trùng kèm lý do — luật chặn thật nằm ở
 * {@link com.sep.vox.application.port.input.service.ExamScheduleCandidateConflictValidator}, chạy
 * trong transaction lúc ghi. Đây không phải lớp bảo vệ.
 */
@Service
public class ViewStudentBusySlotsUseCase
        implements IUseCase<ViewStudentBusySlotsQuery, List<StudentBusySlotResponse>> {

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamDirectoryAccessService examDirectoryAccessService;

    public ViewStudentBusySlotsUseCase(
            ExamScheduleRepository examScheduleRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamDirectoryAccessService examDirectoryAccessService) {
        this.examScheduleRepository = examScheduleRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examDirectoryAccessService = examDirectoryAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentBusySlotResponse> execute(ViewStudentBusySlotsQuery input) {
        if (input.studentIds() == null || input.studentIds().isEmpty()
                || input.scheduleIds() == null || input.scheduleIds().isEmpty()) {
            return List.of();
        }

        var slots = new ArrayList<StudentBusySlotResponse>();
        for (var scheduleId : input.scheduleIds()) {
            var schedule = examScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
            // Cùng luật quyền với danh bạ học sinh: người xem được danh sách thì cũng được biết ai
            // trong số đó đang bận.
            examDirectoryAccessService.resolveByExamId(schedule.getExamId());

            // Loại chính ca đang xét: học sinh đã ở trong ca này thì không phải "bận" dưới góc nhìn
            // của màn chọn — họ đang ở đúng chỗ.
            examCandidateRepository.findConflictsForStudents(
                    input.studentIds(), schedule.getStartDate(), schedule.getEndDate(), schedule.getId())
                .forEach(conflict -> slots.add(new StudentBusySlotResponse(
                    conflict.studentId(),
                    schedule.getId(),
                    conflict.scheduleId(),
                    conflict.startDate() == null ? null : conflict.startDate().toString(),
                    conflict.endDate() == null ? null : conflict.endDate().toString())));
        }
        return slots;
    }
}
