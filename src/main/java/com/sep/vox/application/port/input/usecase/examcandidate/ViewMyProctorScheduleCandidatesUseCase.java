package com.sep.vox.application.port.input.usecase.examcandidate;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.ProctorScheduleAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.ProctorCandidateSummary;
import com.sep.vox.application.query.repository.ProctorScheduleCandidatesQueryRepository;

@Service
public class ViewMyProctorScheduleCandidatesUseCase implements IUseCase<UUID, List<ProctorCandidateSummary>> {

    private final ProctorScheduleCandidatesQueryRepository proctorScheduleCandidatesQueryRepository;
    private final ProctorScheduleAccessService accessService;

    public ViewMyProctorScheduleCandidatesUseCase(
            ProctorScheduleCandidatesQueryRepository proctorScheduleCandidatesQueryRepository,
            ProctorScheduleAccessService accessService) {
        this.proctorScheduleCandidatesQueryRepository = proctorScheduleCandidatesQueryRepository;
        this.accessService = accessService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProctorCandidateSummary> execute(UUID scheduleId) {
        // Khối kiểm quyền từng nằm ngay trong lớp này; nó chuyển sang ProctorScheduleAccessService
        // khi đường đọc thứ hai theo ca thi xuất hiện (lịch sử cảnh báo giám sát), để hai đường không
        // trả lời khác nhau cho cùng câu hỏi "ai được xem ca thi này".
        var schedule = accessService.requireCanMonitorSchedule(scheduleId);
        return proctorScheduleCandidatesQueryRepository.findByScheduleId(schedule.getId());
    }
}
