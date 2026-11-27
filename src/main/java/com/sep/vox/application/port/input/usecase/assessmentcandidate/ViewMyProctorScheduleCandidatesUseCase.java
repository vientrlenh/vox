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
        // The permission block used to live in this class; it moved to ProctorScheduleAccessService when the second
        // read path per assessment schedule appeared (the proctoring warning history), so that the two paths
        // cannot answer differently for the same question "who may view this assessment schedule".
        AssessmentSchedule schedule = accessService.requireCanMonitorSchedule(scheduleId);
        return proctorScheduleCandidatesQueryRepository.findByScheduleId(schedule.getId());
    }
}
