package com.sep.vox.application.port.input.usecase.proctoring;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.ProctorScheduleAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentProctoringAlertDto;
import com.sep.vox.domain.mapper.AssessmentProctoringAlertDtoMapper;
import com.sep.vox.domain.repository.AssessmentProctoringAlertRepository;

/**
 * The proctoring alerts of a whole assessment schedule -- for the live monitoring screen.
 *
 * <p>Exists because that screen takes the SCHEDULE as its unit, not the session: a room has many candidates at the
 * same time, and asking session by session would become N requests every time a room is opened.
 *
 * <p>This is what fixes the hole "a proctor who joins late loses all earlier alerts": the live channel is Redis
 * pub/sub, fire-and-forget, so it can only tell what happens AFTER someone connects. Reading the history from here
 * and merging it with the live stream gives the full picture, and also means reloading the page no longer wipes
 * everything.
 */
@Service
@RequiredArgsConstructor
public class ViewScheduleProctoringAlertsUseCase implements IUseCase<UUID, List<AssessmentProctoringAlertDto>> {

    private final ProctorScheduleAccessService accessService;
    private final AssessmentProctoringAlertRepository assessmentProctoringAlertRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentProctoringAlertDto> execute(UUID scheduleId) {
        AssessmentSchedule schedule = accessService.requireCanMonitorSchedule(scheduleId);
        return assessmentProctoringAlertRepository.findByScheduleIdOrderByCapturedAt(schedule.getId())
            .stream()
            .map(AssessmentProctoringAlertDtoMapper::toDto)
            .toList();
    }
}
