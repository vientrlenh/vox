package com.sep.vox.application.usecase.examsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.examsession.ViewMyExamResultsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;

/**
 * Danh sách kết quả của học sinh dùng chung một luật hiển thị với màn chi tiết. Trước đợt
 * này vị từ bị chép ra hai bản và cả hai đều bỏ sót PASSED/FAILED — hai nơi suy luật là
 * sớm muộn hai nơi lệch nhau.
 */
class ViewMyExamResultsUseCaseTests {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID BAND_ID = UUID.randomUUID();

    private ExamCandidateResultRepository examCandidateResultRepository;
    private ViewMyExamResultsUseCase useCase;

    @BeforeEach
    void setUp() {
        var examCandidateRepository = mock(ExamCandidateRepository.class);
        var examSessionRepository = mock(ExamSessionRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        var examRepository = mock(ExamRepository.class);
        var rubricResultBandRepository = mock(RubricResultBandRepository.class);
        var userContextPort = mock(UserContextPort.class);
        useCase = new ViewMyExamResultsUseCase(
            examCandidateRepository,
            examSessionRepository,
            examCandidateResultRepository,
            examRepository,
            rubricResultBandRepository,
            userContextPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);
        when(examCandidateRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(new ExamCandidate(
            CANDIDATE_ID, EXAM_ID, STUDENT_ID, null, null, ExamCandidateStatus.ATTENDED,
            Instant.now(), Instant.now(), null, null, null)));
        var exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setCode("CT-01");
        exam.setName("Bài tập 1");
        exam.setKind(ExamKind.CLASS_TEST);
        when(examRepository.findByIdIn(List.of(EXAM_ID))).thenReturn(List.of(exam));
        when(examSessionRepository.findLatestByCandidateId(CANDIDATE_ID)).thenReturn(Optional.of(
            new ExamSession(SESSION_ID, EXAM_ID, CANDIDATE_ID, UUID.randomUUID(),
                Instant.now(), Instant.now(), ExamSessionStatus.GRADED, false, null)));
        when(rubricResultBandRepository.findById(BAND_ID)).thenReturn(Optional.empty());
    }

    @Test
    void should_hide_total_score_when_result_pending_review() {
        givenResult(ExamCandidateResultStatus.PENDING_REVIEW);

        var summaries = useCase.execute(null);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).totalScore()).isNull();
        assertThat(summaries.get(0).rubricResultBandId()).isNull();
        // Trạng thái vẫn trả về để danh sách hiển thị được "đang chờ chấm".
        assertThat(summaries.get(0).resultStatus()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void should_show_total_score_when_result_released() {
        givenResult(ExamCandidateResultStatus.RELEASED);

        var summaries = useCase.execute(null);

        assertThat(summaries.get(0).totalScore()).isEqualByComparingTo("7.50");
        assertThat(summaries.get(0).rubricResultBandId()).isEqualTo(BAND_ID);
    }

    /** Vị từ cũ bỏ sót PASSED nên bài đã chốt lại bị giấu điểm khỏi chính chủ. */
    @Test
    void should_show_total_score_when_result_passed() {
        givenResult(ExamCandidateResultStatus.PASSED);

        assertThat(useCase.execute(null).get(0).totalScore()).isEqualByComparingTo("7.50");
    }

    @Test
    void should_hide_total_score_while_the_appeal_is_being_processed() {
        givenResult(ExamCandidateResultStatus.APPEALED);

        assertThat(useCase.execute(null).get(0).totalScore()).isNull();
    }

    private void givenResult(ExamCandidateResultStatus status) {
        var result = new ExamCandidateResult(
            UUID.randomUUID(), EXAM_ID, CANDIDATE_ID, SESSION_ID, UUID.randomUUID(), 1,
            UUID.randomUUID(), UUID.randomUUID(), null, BAND_ID,
            new BigDecimal("7.50"), status, null, null, Instant.now(), Instant.now(), null, null);
        when(examCandidateResultRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(result));
    }
}
