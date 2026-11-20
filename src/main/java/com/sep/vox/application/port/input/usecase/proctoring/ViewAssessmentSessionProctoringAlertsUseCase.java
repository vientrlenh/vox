package com.sep.vox.application.port.input.usecase.proctoring;

import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.AssessmentRecordingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentProctoringAlertDto;
import com.sep.vox.domain.mapper.AssessmentProctoringAlertDtoMapper;
import com.sep.vox.domain.repository.AssessmentProctoringAlertRepository;

/**
 * The proctoring alerts of an assessment session, in chronological order.
 *
 * <p>Shares {@link AssessmentRecordingAccessService} with the recording viewing path and builds no separate permission
 * rule. These are the same kind of evidence about the same person, so whoever can watch the video can
 * see the alerts, and vice versa -- two separate rules for the same question will sooner or later diverge, and
 * a divergence toward the looser side leaks the learner's behavioral data.
 *
 * <p><b>Not done yet:</b> mapping {@code capturedAt} to a seek position in the video. That needs the start time
 * of the recording, which {@code AssessmentRecording} does not currently store: {@code createdAt} is when the data row was created and
 * {@code assembledAt} is when assembly finished, neither is the start of the media. The source exists in
 * vox-streaming (derivable from {@code StreamEndedEvent.EndedAt - Duration} for the WebRTC path, and
 * {@code UploadSession.CreatedAt} for the desktop path), but bringing it here requires changing the contract of
 * {@code RecordingPartChangedEvent} -- i.e. touching the recording assembly path, a change with a very different
 * blast radius that deserves to go on its own.
 */
@Service
public class ViewAssessmentSessionProctoringAlertsUseCase implements IUseCase<UUID, List<AssessmentProctoringAlertDto>> {

    private final AssessmentRecordingAccessService accessService;
    private final AssessmentProctoringAlertRepository assessmentProctoringAlertRepository;

    public ViewAssessmentSessionProctoringAlertsUseCase(
            AssessmentRecordingAccessService accessService,
            AssessmentProctoringAlertRepository assessmentProctoringAlertRepository) {
        this.accessService = accessService;
        this.assessmentProctoringAlertRepository = assessmentProctoringAlertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentProctoringAlertDto> execute(UUID assessmentSessionId) {
        AssessmentSession session = accessService.requireCanViewRecordings(assessmentSessionId);
        return assessmentProctoringAlertRepository.findByAssessmentSessionIdOrderByCapturedAt(session.getId())
            .stream()
            .map(AssessmentProctoringAlertDtoMapper::toDto)
            .toList();
    }
}
