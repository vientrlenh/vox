package com.sep.vox.application.usecase.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewMyExamsQuery;
import com.sep.vox.application.port.input.usecase.exam.ViewMyExamsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ExamAttemptSummary;
import com.sep.vox.application.query.dto.StudentExamRowInfo;
import com.sep.vox.application.query.repository.ExamCandidateAttemptsQueryRepository;
import com.sep.vox.application.query.repository.StudentExamQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;

/**
 * Phần còn lại trong Java sau khi lọc/sắp/phân trang đã xuống SQL: dựng response và quyết định học
 * sinh có được vào phòng thi hay không.
 *
 * <p>Các luật về phạm vi và thứ tự (ẩn kỳ thi DRAFT, ẩn ca chưa publish, mới trước cũ sau, phân
 * trang từ 1) nay nằm trong JPQL nên được kiểm ở
 * {@code JpaStudentExamQueryRepositoryTests} trên DB thật -- giả lập repository ở đây rồi khẳng
 * định lại chúng thì chỉ là kiểm chính cái mock.
 */
class ViewMyExamsUseCaseTests {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private StudentExamQueryRepository studentExamQueryRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamCandidateAttemptsQueryRepository attemptsQueryRepository;
    private ViewMyExamsUseCase useCase;

    @BeforeEach
    void setUp() {
        studentExamQueryRepository = mock(StudentExamQueryRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        attemptsQueryRepository = mock(ExamCandidateAttemptsQueryRepository.class);
        var examPaperRepository = mock(ExamPaperRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new ViewMyExamsUseCase(
            studentExamQueryRepository,
            examPaperRepository,
            examScheduleRepository,
            attemptsQueryRepository,
            userContextPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);
        when(examPaperRepository.findByIdIn(anyCollection())).thenReturn(List.of());
        when(attemptsQueryRepository.findByCandidateIds(anyCollection())).thenReturn(List.of());
        // Ca thi còn hiệu lực -- các ca kiểm tra riêng sẽ ghi đè khi cần.
        when(examScheduleRepository.findByIdAndInSchedule(any(), any()))
            .thenReturn(Optional.of(new ExamSchedule()));
    }

    @Test
    void should_pass_the_paging_envelope_through_untouched() {
        givenRows(new PageResult<>(List.of(row().build()), 2, 20, 41, 3));

        var result = useCase.execute(new ViewMyExamsQuery(null, null, 2, 20, true));

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(41);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void should_forward_the_one_based_page_to_the_query_repository() {
        givenRows(new PageResult<>(List.of(), 1, 20, 0, 0));

        useCase.execute(new ViewMyExamsQuery(null, null, 1, 20, true));

        org.mockito.Mockito.verify(studentExamQueryRepository)
            .findMyExams(eq(STUDENT_ID), eq(null), eq(null), eq(true), eq(1), eq(20), any());
    }

    @Test
    void should_let_an_attended_student_of_a_running_exam_enter() {
        givenRows(row().candidateStatus(ExamCandidateStatus.ATTENDED).examStatus(ExamStatus.IN_PROGRESS).build());

        var response = firstResponse();

        assertThat(response.canEnter()).isTrue();
        assertThat(response.entryMessage()).isNull();
    }

    @Test
    void should_block_a_student_who_was_forced_to_stop() {
        givenRows(row()
            .candidateStatus(ExamCandidateStatus.ATTENDED)
            .examStatus(ExamStatus.IN_PROGRESS)
            .blockedAt(NOW)
            .build());

        assertThat(firstResponse().canEnter()).isFalse();
        assertThat(firstResponse().entryMessage()).contains("buộc kết thúc");
    }

    @Test
    void should_block_a_student_who_was_not_marked_present() {
        givenRows(row().candidateStatus(ExamCandidateStatus.ASSIGNED).examStatus(ExamStatus.IN_PROGRESS).build());

        assertThat(firstResponse().entryMessage()).contains("điểm danh");
    }

    @Test
    void should_block_when_the_exam_is_not_running_yet() {
        givenRows(row().candidateStatus(ExamCandidateStatus.ATTENDED).examStatus(ExamStatus.SCHEDULED).build());

        assertThat(firstResponse().entryMessage()).contains("chưa được mở");
    }

    @Test
    void should_block_when_the_student_has_no_assigned_paper() {
        givenRows(row()
            .candidateStatus(ExamCandidateStatus.ATTENDED)
            .examStatus(ExamStatus.IN_PROGRESS)
            .assignedPaperId(null)
            .build());

        assertThat(firstResponse().entryMessage()).contains("chưa được gán đề thi");
    }

    /**
     * Session còn dở (IN_PROGRESS/INTERRUPTED) là phiên vào lại được, không phải một lượt đã tiêu.
     * Đếm nhầm thì học sinh bị báo "hết lượt" ngay ở màn danh sách, trước khi kịp vào lại phiên dở.
     */
    @Test
    void should_not_count_an_unfinished_session_as_a_used_attempt() {
        var candidateId = UUID.randomUUID();
        givenRows(row().candidateId(candidateId).candidateStatus(ExamCandidateStatus.ATTENDED)
            .examStatus(ExamStatus.IN_PROGRESS).maxAttempt(1).build());
        when(attemptsQueryRepository.findByCandidateIds(anyCollection()))
            .thenReturn(List.of(attempt(candidateId, ExamSessionStatus.IN_PROGRESS)));

        var response = firstResponse();

        assertThat(response.attemptsUsed()).isZero();
        assertThat(response.canEnter()).isTrue();
    }

    @Test
    void should_block_once_every_attempt_is_used_up() {
        var candidateId = UUID.randomUUID();
        givenRows(row().candidateId(candidateId).candidateStatus(ExamCandidateStatus.ATTENDED)
            .examStatus(ExamStatus.IN_PROGRESS).maxAttempt(1).build());
        when(attemptsQueryRepository.findByCandidateIds(anyCollection()))
            .thenReturn(List.of(attempt(candidateId, ExamSessionStatus.SUBMITTED)));

        var response = firstResponse();

        assertThat(response.attemptsUsed()).isEqualTo(1);
        assertThat(response.entryMessage()).contains("hết số lượt");
    }

    /** Không có đề thì thời lượng suy từ độ dài ca thi. */
    @Test
    void should_derive_the_duration_from_the_schedule_window() {
        givenRows(row().scheduleStartDate(NOW).scheduleEndDate(NOW.plusSeconds(2700)).build());

        assertThat(firstResponse().duration()).isEqualTo(45);
    }

    @Test
    void should_expose_the_kind_as_a_readable_subject() {
        givenRows(row().examKind("CLASS_TEST").build());

        assertThat(firstResponse().subject()).isEqualTo("CLASS TEST");
    }

    // ---- fixtures ----------------------------------------------------------

    private void givenRows(StudentExamRowInfo... rows) {
        givenRows(new PageResult<>(List.of(rows), 1, 20, rows.length, 1));
    }

    private void givenRows(PageResult<StudentExamRowInfo> page) {
        when(studentExamQueryRepository.findMyExams(
            any(), any(), any(), anyBoolean(), anyInt(), anyInt(), any())).thenReturn(page);
    }

    private com.sep.vox.application.response.input.exam.StudentExamSummaryResponse firstResponse() {
        return useCase.execute(new ViewMyExamsQuery(null, null, 1, 20, true)).content().get(0);
    }

    private static ExamAttemptSummary attempt(UUID candidateId, ExamSessionStatus status) {
        return new ExamAttemptSummary(
            candidateId, UUID.randomUUID(), ExamCandidateStatus.ATTENDED, UUID.randomUUID(),
            NOW, null, status, false, null, BigDecimal.ZERO,
            null, null, null, null, null, ExamCandidateResultStatus.PENDING_REVIEW);
    }

    private static RowBuilder row() {
        return new RowBuilder();
    }

    /** Dòng "bình thường": đã điểm danh, kỳ thi đang mở, ca thi đang diễn ra, đã có đề. */
    private static final class RowBuilder {
        private UUID candidateId = UUID.randomUUID();
        private ExamCandidateStatus candidateStatus = ExamCandidateStatus.ATTENDED;
        private Instant blockedAt;
        private UUID assignedPaperId = UUID.randomUUID();
        private String examKind = "CENTRALIZED";
        private ExamStatus examStatus = ExamStatus.IN_PROGRESS;
        private Integer maxAttempt = 3;
        private Instant scheduleStartDate = NOW.minusSeconds(600);
        private Instant scheduleEndDate = NOW.plusSeconds(600);

        RowBuilder candidateId(UUID value) {
            this.candidateId = value;
            return this;
        }

        RowBuilder candidateStatus(ExamCandidateStatus value) {
            this.candidateStatus = value;
            return this;
        }

        RowBuilder blockedAt(Instant value) {
            this.blockedAt = value;
            return this;
        }

        RowBuilder assignedPaperId(UUID value) {
            this.assignedPaperId = value;
            return this;
        }

        RowBuilder examKind(String value) {
            this.examKind = value;
            return this;
        }

        RowBuilder examStatus(ExamStatus value) {
            this.examStatus = value;
            return this;
        }

        RowBuilder maxAttempt(Integer value) {
            this.maxAttempt = value;
            return this;
        }

        RowBuilder scheduleStartDate(Instant value) {
            this.scheduleStartDate = value;
            return this;
        }

        RowBuilder scheduleEndDate(Instant value) {
            this.scheduleEndDate = value;
            return this;
        }

        StudentExamRowInfo build() {
            return new StudentExamRowInfo(
                candidateId, candidateStatus, blockedAt, assignedPaperId,
                UUID.randomUUID(), "Kỳ thi", "Mô tả", examKind, examStatus, true, maxAttempt,
                UUID.randomUUID(), scheduleStartDate, scheduleEndDate,
                scheduleStartDate, "in_progress");
        }
    }
}
