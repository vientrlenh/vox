package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.service.AssessmentCandidateResultFinalizationService;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

class AssessmentCandidateResultFinalizationServiceTests {

    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentResultAppealRepository assessmentResultAppealRepository;
    private AssessmentCandidateResultFinalizationService service;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID resultId = UUID.randomUUID();
    private final UUID actingUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentResultAppealRepository = mock(AssessmentResultAppealRepository.class);
        service = new AssessmentCandidateResultFinalizationService(
            assessmentCandidateResultRepository, assessmentResultAppealRepository);
    }

    private AssessmentCandidateResult releasedResult() {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(resultId);
        result.setSessionId(sessionId);
        result.setStatus(AssessmentCandidateResultStatus.RELEASED);
        return result;
    }

    private AssessmentResultAppeal appeal(AssessmentAppealStatus status) {
        AssessmentResultAppeal appeal = new AssessmentResultAppeal();
        appeal.setId(UUID.randomUUID());
        appeal.setCandidateResultId(resultId);
        appeal.setStatus(status);
        return appeal;
    }

    @Test
    void should_invalidate_result_and_zero_score_when_late_violation_found() {
        when(assessmentCandidateResultRepository.findBySessionId(sessionId))
            .thenReturn(Optional.of(releasedResult()));
        when(assessmentResultAppealRepository.findByCandidateResultId(resultId)).thenReturn(List.of());

        service.invalidateNow(sessionId, actingUserId);

        ArgumentCaptor<AssessmentCandidateResult> captor = ArgumentCaptor.forClass(AssessmentCandidateResult.class);
        verify(assessmentCandidateResultRepository).save(captor.capture());
        AssessmentCandidateResult saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AssessmentCandidateResultStatus.INVALID);
        assertThat(saved.getTotalScore()).isEqualByComparingTo("0.00");
    }

    @Test
    void should_close_open_appeals_when_result_is_invalidated() {
        when(assessmentCandidateResultRepository.findBySessionId(sessionId))
            .thenReturn(Optional.of(releasedResult()));
        AssessmentResultAppeal openAppeal = appeal(AssessmentAppealStatus.GRADING);
        when(assessmentResultAppealRepository.findByCandidateResultId(resultId))
            .thenReturn(List.of(openAppeal));

        service.invalidateNow(sessionId, actingUserId);

        // An open appeal must be closed (REJECTED) so that publishing cannot "resurrect" the
        // submission.
        assertThat(openAppeal.getStatus()).isEqualTo(AssessmentAppealStatus.REJECTED);
        assertThat(openAppeal.getResolvedAt()).isNotNull();
        verify(assessmentResultAppealRepository).save(openAppeal);
    }

    @Test
    void should_not_touch_already_terminal_appeals() {
        when(assessmentCandidateResultRepository.findBySessionId(sessionId))
            .thenReturn(Optional.of(releasedResult()));
        AssessmentResultAppeal published = appeal(AssessmentAppealStatus.PUBLISHED);
        AssessmentResultAppeal rejected = appeal(AssessmentAppealStatus.REJECTED);
        when(assessmentResultAppealRepository.findByCandidateResultId(resultId))
            .thenReturn(List.of(published, rejected));

        service.invalidateNow(sessionId, actingUserId);

        assertThat(published.getStatus()).isEqualTo(AssessmentAppealStatus.PUBLISHED);
        assertThat(rejected.getStatus()).isEqualTo(AssessmentAppealStatus.REJECTED);
        verify(assessmentResultAppealRepository, never()).save(any());
    }

    @Test
    void should_do_nothing_when_session_has_no_result() {
        when(assessmentCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        service.invalidateNow(sessionId, actingUserId);

        verify(assessmentCandidateResultRepository, never()).save(any());
        verify(assessmentResultAppealRepository, never()).save(any());
    }
}
