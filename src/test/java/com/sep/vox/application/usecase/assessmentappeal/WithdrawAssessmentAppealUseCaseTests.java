package com.sep.vox.application.usecase.assessmentappeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.assessmentappeal.WithdrawAssessmentAppealUseCase;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.AssessmentResultStatusHistoryRepository;

class WithdrawAssessmentAppealUseCaseTests {

    private AssessmentResultAppealRepository assessmentResultAppealRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentAppealAccessService assessmentAppealAccessService;
    private WithdrawAssessmentAppealUseCase useCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    private AssessmentResultAppeal appeal;
    private AssessmentCandidateResult candidateResult;

    @BeforeEach
    void setUp() {
        assessmentResultAppealRepository = mock(AssessmentResultAppealRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentAppealAccessService = mock(AssessmentAppealAccessService.class);
        useCase = new WithdrawAssessmentAppealUseCase(
            assessmentResultAppealRepository, assessmentCandidateResultRepository, assessmentAppealAccessService,
            new ResultStatusHistoryRecorder(mock(AssessmentResultStatusHistoryRepository.class)));

        appeal = new AssessmentResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(AssessmentAppealStatus.PENDING);

        candidateResult = new AssessmentCandidateResult();
        candidateResult.setId(UUID.randomUUID());
        candidateResult.setStatus(AssessmentCandidateResultStatus.APPEALED);

        when(assessmentAppealAccessService.requireActiveUserId()).thenReturn(studentId);
        when(assessmentAppealAccessService.load(appealId)).thenReturn(new AppealContext(
            appeal, candidateResult, new AssessmentSession(), UUID.randomUUID(), studentId, "IELTS Mock"));
    }

    @Test
    void should_withdraw_and_restore_the_result_to_released() {
        useCase.execute(appealId);

        assertThat(appeal.getStatus()).isEqualTo(AssessmentAppealStatus.WITHDRAWN);
        assertThat(appeal.getWithdrawnAt()).isNotNull();
        assertThat(candidateResult.getStatus()).isEqualTo(AssessmentCandidateResultStatus.RELEASED);
    }

    @Test
    void should_not_burn_an_appeal_round() {
        useCase.execute(appealId);

        // Hạn mức đếm số đơn đã PUBLISHED; WITHDRAWN không nằm trong đó, nên học sinh
        // rút đơn rồi vẫn còn nguyên lượt.
        assertThat(appeal.getStatus()).isNotEqualTo(AssessmentAppealStatus.PUBLISHED);
    }

    @Test
    void should_stop_blocking_a_new_appeal() {
        useCase.execute(appealId);

        // isOpen() is what CreateAssessmentAppealUseCase asks to block a duplicate appeal.
        assertThat(appeal.isOpen()).isFalse();
    }

    @Test
    void should_not_pull_a_result_that_already_moved_on_back_to_released() {
        // Bài đã chốt sổ (hoặc đã sang PASSED/FAILED) thì một thao tác của học sinh
        // không được phép mở lại nó — rút đơn chỉ đóng đơn.
        candidateResult.setStatus(AssessmentCandidateResultStatus.FINAL);

        useCase.execute(appealId);

        assertThat(appeal.getStatus()).isEqualTo(AssessmentAppealStatus.WITHDRAWN);
        assertThat(candidateResult.getStatus()).isEqualTo(AssessmentCandidateResultStatus.FINAL);
        verify(assessmentCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_restore_a_result_that_is_still_being_re_graded() {
        candidateResult.setStatus(AssessmentCandidateResultStatus.RE_GRADING);

        useCase.execute(appealId);

        assertThat(candidateResult.getStatus()).isEqualTo(AssessmentCandidateResultStatus.RELEASED);
    }

    @Test
    void should_reject_withdrawing_after_the_appeal_was_approved() {
        appeal.setStatus(AssessmentAppealStatus.APPROVED);

        // Từ APPROVED trở đi đã có người bị điều động và có thể đã đọc bài; muốn dừng
        // thì đi đường admin từ chối, có ghi lý do.
        assertThatThrownBy(() -> useCase.execute(appealId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chờ duyệt");

        verify(assessmentResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_withdrawing_someone_elses_appeal() {
        org.mockito.Mockito.doThrow(new ForbiddenException("BẢO MẬT"))
            .when(assessmentAppealAccessService).authorizeOwningStudent(any(), any());

        assertThatThrownBy(() -> useCase.execute(appealId))
            .isInstanceOf(ForbiddenException.class);

        verify(assessmentResultAppealRepository, never()).save(any());
    }
}
