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

import com.sep.vox.application.port.input.service.ExamCandidateResultFinalizationService;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

class ExamCandidateResultFinalizationServiceTests {

    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamResultAppealRepository examResultAppealRepository;
    private ExamCandidateResultFinalizationService service;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID resultId = UUID.randomUUID();
    private final UUID actingUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        service = new ExamCandidateResultFinalizationService(
            examCandidateResultRepository, examResultAppealRepository);
    }

    private ExamCandidateResult releasedResult() {
        var result = new ExamCandidateResult();
        result.setId(resultId);
        result.setSessionId(sessionId);
        result.setStatus(ExamCandidateResultStatus.RELEASED);
        return result;
    }

    private ExamResultAppeal appeal(ExamAppealStatus status) {
        var appeal = new ExamResultAppeal();
        appeal.setId(UUID.randomUUID());
        appeal.setCandidateResultId(resultId);
        appeal.setStatus(status);
        return appeal;
    }

    @Test
    void should_invalidate_result_and_zero_score_when_late_violation_found() {
        when(examCandidateResultRepository.findBySessionId(sessionId))
            .thenReturn(Optional.of(releasedResult()));
        when(examResultAppealRepository.findByCandidateResultId(resultId)).thenReturn(List.of());

        service.invalidateNow(sessionId, actingUserId);

        var captor = ArgumentCaptor.forClass(ExamCandidateResult.class);
        verify(examCandidateResultRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ExamCandidateResultStatus.INVALID);
        assertThat(saved.getTotalScore()).isEqualByComparingTo("0.00");
    }

    @Test
    void should_close_open_appeals_when_result_is_invalidated() {
        when(examCandidateResultRepository.findBySessionId(sessionId))
            .thenReturn(Optional.of(releasedResult()));
        var openAppeal = appeal(ExamAppealStatus.COMPARING);
        when(examResultAppealRepository.findByCandidateResultId(resultId))
            .thenReturn(List.of(openAppeal));

        service.invalidateNow(sessionId, actingUserId);

        // Đơn phúc khảo đang mở phải bị đóng (REJECTED) để không thể publish "hồi sinh" bài.
        assertThat(openAppeal.getStatus()).isEqualTo(ExamAppealStatus.REJECTED);
        assertThat(openAppeal.getResolvedAt()).isNotNull();
        verify(examResultAppealRepository).save(openAppeal);
    }

    @Test
    void should_not_touch_already_terminal_appeals() {
        when(examCandidateResultRepository.findBySessionId(sessionId))
            .thenReturn(Optional.of(releasedResult()));
        var published = appeal(ExamAppealStatus.PUBLISHED);
        var rejected = appeal(ExamAppealStatus.REJECTED);
        when(examResultAppealRepository.findByCandidateResultId(resultId))
            .thenReturn(List.of(published, rejected));

        service.invalidateNow(sessionId, actingUserId);

        assertThat(published.getStatus()).isEqualTo(ExamAppealStatus.PUBLISHED);
        assertThat(rejected.getStatus()).isEqualTo(ExamAppealStatus.REJECTED);
        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_do_nothing_when_session_has_no_result() {
        when(examCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        service.invalidateNow(sessionId, actingUserId);

        verify(examCandidateResultRepository, never()).save(any());
        verify(examResultAppealRepository, never()).save(any());
    }
}
