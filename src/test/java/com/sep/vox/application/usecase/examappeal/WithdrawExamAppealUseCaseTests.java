package com.sep.vox.application.usecase.examappeal;

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
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.examappeal.WithdrawExamAppealUseCase;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.ExamResultStatusHistoryRepository;

class WithdrawExamAppealUseCaseTests {

    private ExamResultAppealRepository examResultAppealRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamAppealAccessService examAppealAccessService;
    private WithdrawExamAppealUseCase useCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    private ExamResultAppeal appeal;
    private ExamCandidateResult candidateResult;

    @BeforeEach
    void setUp() {
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examAppealAccessService = mock(ExamAppealAccessService.class);
        useCase = new WithdrawExamAppealUseCase(
            examResultAppealRepository, examCandidateResultRepository, examAppealAccessService,
            new ResultStatusHistoryRecorder(mock(ExamResultStatusHistoryRepository.class)));

        appeal = new ExamResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(ExamAppealStatus.PENDING);

        candidateResult = new ExamCandidateResult();
        candidateResult.setId(UUID.randomUUID());
        candidateResult.setStatus(ExamCandidateResultStatus.APPEALED);

        when(examAppealAccessService.requireActiveUserId()).thenReturn(studentId);
        when(examAppealAccessService.load(appealId)).thenReturn(new AppealContext(
            appeal, candidateResult, new ExamSession(), UUID.randomUUID(), studentId, "IELTS Mock"));
    }

    @Test
    void should_withdraw_and_restore_the_result_to_released() {
        useCase.execute(appealId);

        assertThat(appeal.getStatus()).isEqualTo(ExamAppealStatus.WITHDRAWN);
        assertThat(appeal.getWithdrawnAt()).isNotNull();
        assertThat(candidateResult.getStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
    }

    @Test
    void should_not_burn_an_appeal_round() {
        useCase.execute(appealId);

        // Hạn mức đếm số đơn đã PUBLISHED; WITHDRAWN không nằm trong đó, nên học sinh
        // rút đơn rồi vẫn còn nguyên lượt.
        assertThat(appeal.getStatus()).isNotEqualTo(ExamAppealStatus.PUBLISHED);
    }

    @Test
    void should_stop_blocking_a_new_appeal() {
        useCase.execute(appealId);

        // isOpen() là thứ CreateExamAppealUseCase hỏi để chặn đơn trùng.
        assertThat(appeal.isOpen()).isFalse();
    }

    @Test
    void should_not_pull_a_result_that_already_moved_on_back_to_released() {
        // Bài đã chốt sổ (hoặc đã sang PASSED/FAILED) thì một thao tác của học sinh
        // không được phép mở lại nó — rút đơn chỉ đóng đơn.
        candidateResult.setStatus(ExamCandidateResultStatus.FINAL);

        useCase.execute(appealId);

        assertThat(appeal.getStatus()).isEqualTo(ExamAppealStatus.WITHDRAWN);
        assertThat(candidateResult.getStatus()).isEqualTo(ExamCandidateResultStatus.FINAL);
        verify(examCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_restore_a_result_that_is_still_being_re_graded() {
        candidateResult.setStatus(ExamCandidateResultStatus.RE_GRADING);

        useCase.execute(appealId);

        assertThat(candidateResult.getStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
    }

    @Test
    void should_reject_withdrawing_after_the_appeal_was_approved() {
        appeal.setStatus(ExamAppealStatus.APPROVED);

        // Từ APPROVED trở đi đã có người bị điều động và có thể đã đọc bài; muốn dừng
        // thì đi đường admin từ chối, có ghi lý do.
        assertThatThrownBy(() -> useCase.execute(appealId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chờ duyệt");

        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_reject_withdrawing_someone_elses_appeal() {
        org.mockito.Mockito.doThrow(new ForbiddenException("BẢO MẬT"))
            .when(examAppealAccessService).authorizeOwningStudent(any(), any());

        assertThatThrownBy(() -> useCase.execute(appealId))
            .isInstanceOf(ForbiddenException.class);

        verify(examResultAppealRepository, never()).save(any());
    }
}
