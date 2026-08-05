package com.sep.vox.application.usecase.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewMyExamsQuery;
import com.sep.vox.application.port.input.usecase.exam.ViewMyExamsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.ExamCandidateAttemptsQueryRepository;
import com.sep.vox.application.response.input.exam.StudentExamSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;

/**
 * Màn danh sách bài thi của học sinh trước đây trả về mọi bài mà em đó là thí sinh, kể cả kỳ thi
 * còn DRAFT và bài chưa được xếp ca -- tức lịch chưa xếp xong đã lộ ra cho học sinh. Thứ tự lại là
 * cũ trước mới sau.
 */
class ViewMyExamsUseCaseTests {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private ExamCandidateRepository examCandidateRepository;
    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ViewMyExamsUseCase useCase;

    private final List<ExamCandidate> candidates = new ArrayList<>();
    private final List<Exam> exams = new ArrayList<>();
    private final List<ExamSchedule> schedules = new ArrayList<>();

    @BeforeEach
    void setUp() {
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examRepository = mock(ExamRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        var examPaperRepository = mock(ExamPaperRepository.class);
        var attemptsQueryRepository = mock(ExamCandidateAttemptsQueryRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new ViewMyExamsUseCase(
            examCandidateRepository,
            examRepository,
            examPaperRepository,
            examScheduleRepository,
            attemptsQueryRepository,
            userContextPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);
        when(examPaperRepository.findByIdIn(anyCollection())).thenReturn(List.of());
        when(attemptsQueryRepository.findByCandidateIds(anyCollection())).thenReturn(List.of());
        when(examCandidateRepository.findByStudentId(STUDENT_ID)).thenReturn(candidates);
        when(examRepository.findByIdIn(anyCollection())).thenAnswer(invocation ->
            filterByIds(exams, invocation.getArgument(0), exam -> exam.getId()));
        when(examScheduleRepository.findByIdIn(anyCollection())).thenAnswer(invocation ->
            filterByIds(schedules, invocation.getArgument(0), schedule -> schedule.getId()));
    }

    @Test
    void should_hide_exam_in_draft_status() {
        givenExam("Kỳ thi nháp", ExamStatus.DRAFT, ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(3600));

        assertThat(execute().content()).isEmpty();
    }

    @Test
    void should_hide_exam_when_candidate_has_no_schedule() {
        var candidate = givenExam("Chưa xếp ca", ExamStatus.SCHEDULED, ExamScheduleStatus.PUBLISHED,
            NOW.plusSeconds(3600));
        candidate.setScheduleId(null);

        assertThat(execute().content()).isEmpty();
    }

    @Test
    void should_hide_exam_when_schedule_is_draft() {
        givenExam("Ca chưa publish", ExamStatus.SCHEDULED, ExamScheduleStatus.DRAFT, NOW.plusSeconds(3600));

        assertThat(execute().content()).isEmpty();
    }

    @Test
    void should_hide_exam_when_schedule_is_moved() {
        givenExam("Ca đã dời", ExamStatus.SCHEDULED, ExamScheduleStatus.MOVED, NOW.plusSeconds(3600));

        assertThat(execute().content()).isEmpty();
    }

    /** Kỳ thi bị huỷ vẫn phải hiện, nếu không học sinh cứ chờ một ca đã không còn. */
    @Test
    void should_keep_cancelled_exam() {
        givenExam("Kỳ thi đã huỷ", ExamStatus.CANCELLED, ExamScheduleStatus.CANCELLED, NOW.plusSeconds(3600));

        assertThat(execute().content()).extracting(response -> response.title())
            .containsExactly("Kỳ thi đã huỷ");
    }

    @Test
    void should_sort_exams_from_newest_to_oldest() {
        givenExam("Bài cũ", ExamStatus.CLOSED, ExamScheduleStatus.COMPLETED, NOW.minusSeconds(86400));
        givenExam("Bài mới", ExamStatus.SCHEDULED, ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(86400));
        givenExam("Bài giữa", ExamStatus.SCHEDULED, ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(3600));

        assertThat(execute().content()).extracting(response -> response.title())
            .containsExactly("Bài mới", "Bài giữa", "Bài cũ");
    }

    @Test
    void should_sort_ascending_when_asked() {
        givenExam("Bài cũ", ExamStatus.CLOSED, ExamScheduleStatus.COMPLETED, NOW.minusSeconds(86400));
        givenExam("Bài mới", ExamStatus.SCHEDULED, ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(86400));

        var result = useCase.execute(new ViewMyExamsQuery(null, null, 0, 20, false));

        assertThat(result.content()).extracting(response -> response.title())
            .containsExactly("Bài cũ", "Bài mới");
    }

    /** Ca thi thiếu ngày là dữ liệu lỗi, đảo chiều mà kéo nó lên đầu thì mở màn ra toàn dòng trống. */
    @Test
    void should_put_exam_without_date_last() {
        givenExam("Có ngày", ExamStatus.SCHEDULED, ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(3600));
        givenExam("Không ngày", ExamStatus.SCHEDULED, ExamScheduleStatus.PUBLISHED, null);

        assertThat(execute().content()).extracting(response -> response.title())
            .containsExactly("Có ngày", "Không ngày");
    }

    @Test
    void should_filter_by_kind() {
        givenExam("Kỳ thi tập trung", ExamStatus.SCHEDULED, ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(3600));
        var classTest = givenExam("Bài trên lớp", ExamStatus.SCHEDULED, ExamScheduleStatus.PUBLISHED,
            NOW.plusSeconds(7200));
        examOf(classTest).setKind(ExamKind.CLASS_TEST);

        var result = useCase.execute(new ViewMyExamsQuery(ExamKind.CLASS_TEST, null, 0, 20, true));

        assertThat(result.content()).extracting(response -> response.title()).containsExactly("Bài trên lớp");
    }

    @Test
    void should_filter_by_derived_status() {
        givenExam("Sắp thi", ExamStatus.SCHEDULED, ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(3600));
        givenExam("Đã xong", ExamStatus.CLOSED, ExamScheduleStatus.COMPLETED, NOW.minusSeconds(86400));

        var result = useCase.execute(new ViewMyExamsQuery(null, "completed", 0, 20, true));

        assertThat(result.content()).extracting(response -> response.title()).containsExactly("Đã xong");
    }

    @Test
    void should_paginate_and_report_total_elements() {
        givenExam("Bài 1", ExamStatus.SCHEDULED, ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(300));
        givenExam("Bài 2", ExamStatus.SCHEDULED, ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(200));
        givenExam("Bài 3", ExamStatus.SCHEDULED, ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(100));

        var firstPage = useCase.execute(new ViewMyExamsQuery(null, null, 0, 2, true));
        var secondPage = useCase.execute(new ViewMyExamsQuery(null, null, 1, 2, true));

        assertThat(firstPage.content()).extracting(response -> response.title()).containsExactly("Bài 1", "Bài 2");
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).extracting(response -> response.title()).containsExactly("Bài 3");
    }

    private PageResult<StudentExamSummaryResponse> execute() {
        return useCase.execute(new ViewMyExamsQuery(null, null, 0, 20, true));
    }

    private ExamCandidate givenExam(
            String name,
            ExamStatus examStatus,
            ExamScheduleStatus scheduleStatus,
            Instant startDate) {
        var examId = UUID.randomUUID();
        var scheduleId = UUID.randomUUID();

        var exam = new Exam();
        exam.setId(examId);
        exam.setName(name);
        exam.setKind(ExamKind.CENTRALIZED);
        exam.setStatus(examStatus);
        exam.setMaxAttempt(1);
        exams.add(exam);

        var schedule = new ExamSchedule();
        schedule.setId(scheduleId);
        schedule.setExamId(examId);
        schedule.setStatus(scheduleStatus);
        schedule.setStartDate(startDate);
        schedule.setEndDate(startDate == null ? null : startDate.plusSeconds(3600));
        schedules.add(schedule);

        var candidate = new ExamCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setExamId(examId);
        candidate.setStudentId(STUDENT_ID);
        candidate.setScheduleId(scheduleId);
        candidate.setStatus(ExamCandidateStatus.ASSIGNED);
        candidates.add(candidate);
        return candidate;
    }

    private Exam examOf(ExamCandidate candidate) {
        return exams.stream().filter(exam -> exam.getId().equals(candidate.getExamId())).findFirst().orElseThrow();
    }

    private static <T> List<T> filterByIds(
            List<T> source,
            Collection<UUID> ids,
            java.util.function.Function<T, UUID> idOf) {
        return source.stream().filter(item -> ids.contains(idOf.apply(item))).toList();
    }
}
