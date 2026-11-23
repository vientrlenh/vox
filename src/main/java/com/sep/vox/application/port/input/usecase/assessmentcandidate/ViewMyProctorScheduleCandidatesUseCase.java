package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.ProctorScheduleAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.ProctorCandidateSummary;
import com.sep.vox.application.query.repository.ProctorScheduleCandidatesQueryRepository;

@Service
@RequiredArgsConstructor
public class ViewMyProctorScheduleCandidatesUseCase implements IUseCase<UUID, List<ProctorCandidateSummary>> {

    private final ProctorScheduleCandidatesQueryRepository proctorScheduleCandidatesQueryRepository;
    private final ProctorScheduleAccessService accessService;

    @Override
    @Transactional(readOnly = true)
    public List<ProctorCandidateSummary> execute(UUID scheduleId) {
        // Khối kiểm quyền từng nằm ngay trong lớp này; nó chuyển sang ProctorScheduleAccessService
        // khi đường đọc thứ hai theo ca thi xuất hiện (lịch sử cảnh báo giám sát), để hai đường không
        // trả lời khác nhau cho cùng câu hỏi "ai được xem ca thi này".
        AssessmentSchedule schedule = accessService.requireCanMonitorSchedule(scheduleId);
        return proctorScheduleCandidatesQueryRepository.findByScheduleId(schedule.getId());
    }
}
