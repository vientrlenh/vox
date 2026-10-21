package com.sep.vox.application.port.input.usecase.proctoring;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.ProctorScheduleAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.assessment.AssessmentProctoringAlertDto;
import com.sep.vox.domain.repository.assessment.AssessmentProctoringAlertRepository;

/**
 * Cảnh báo giám sát của cả một ca thi -- cho màn giám sát trực tiếp.
 *
 * <p>Tồn tại vì màn đó lấy CA THI làm đơn vị, không phải phiên thi: một phòng có nhiều thí sinh cùng
 * lúc, và hỏi từng phiên một sẽ thành N request cho mỗi lần mở phòng.
 *
 * <p>Đây là thứ vá được lỗ hổng "giám thị vào muộn thì mất hết cảnh báo trước đó": kênh trực tiếp là
 * Redis pub/sub, fire-and-forget, nên nó chỉ kể được những gì xảy ra SAU khi người ta kết nối. Đọc
 * lịch sử từ đây rồi gộp với luồng trực tiếp cho ra bức tranh đầy đủ, và cũng khiến việc tải lại
 * trang không còn xoá sạch mọi thứ.
 */
@Service
public class ViewScheduleProctoringAlertsUseCase implements IUseCase<UUID, List<AssessmentProctoringAlertDto>> {

    private final ProctorScheduleAccessService accessService;
    private final AssessmentProctoringAlertRepository assessmentProctoringAlertRepository;

    public ViewScheduleProctoringAlertsUseCase(
            ProctorScheduleAccessService accessService,
            AssessmentProctoringAlertRepository assessmentProctoringAlertRepository) {
        this.accessService = accessService;
        this.assessmentProctoringAlertRepository = assessmentProctoringAlertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentProctoringAlertDto> execute(UUID scheduleId) {
        var schedule = accessService.requireCanMonitorSchedule(scheduleId);
        return assessmentProctoringAlertRepository.findByScheduleIdOrderByCapturedAt(schedule.getId())
            .stream()
            .map(AssessmentProctoringAlertDto::toDto)
            .toList();
    }
}
