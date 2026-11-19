package com.sep.vox.application.usecase.examschedule;

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

import com.sep.vox.application.port.input.query.ViewExamSchedulesQuery;
import com.sep.vox.application.port.input.usecase.examschedule.ViewMyExamSchedulesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;

/**
 * Lịch thi của học sinh trước đây chỉ loại ca DELETED, nên ca DRAFT (chưa publish) và ca đã dời vẫn
 * hiện, lại không sắp xếp gì cả.
 */
class ViewMyExamSchedulesUseCaseTests {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private ViewMyExamSchedulesUseCase useCase;
    private ExamRepository examRepository;

    private final List<ExamCandidate> candidates = new ArrayList<>();
    private final List<ExamSchedule> schedules = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ExamCandidateRepository examCandidateRepository = mock(ExamCandidateRepository.class);
        ExamScheduleRepository examScheduleRepository = mock(ExamScheduleRepository.class);
        UserContextPort userContextPort = mock(UserContextPort.class);
        examRepository = mock(ExamRepository.class);

        useCase = new ViewMyExamSchedulesUseCase(
            examCandidateRepository, examScheduleRepository, examRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);
        when(examCandidateRepository.findByStudentId(STUDENT_ID)).thenReturn(candidates);
        // A session only shows when the exam has been published; every session in this test set
        // belongs to a SCHEDULED exam so that the cases test exactly what they mean to test, the
        // SESSION status.
        Exam exam = mock(Exam.class);
        when(exam.getId()).thenReturn(EXAM_ID);
        when(exam.getStatus()).thenReturn(ExamStatus.SCHEDULED);
        when(examRepository.findByIdIn(anyCollection())).thenReturn(List.of(exam));
        when(examScheduleRepository.findByIdIn(anyCollection())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            return schedules.stream().filter(schedule -> ids.contains(schedule.getId())).toList();
        });
    }

    /**
     * A session that is published but whose EXAM is still DRAFT must still be hidden: the two
     * statuses are independent, and the schedule of an unpublished exam must not be exposed to
     * candidates.
     */
    @Test
    void should_hide_schedules_of_draft_exam() {
        Exam draftExam = mock(Exam.class);
        when(draftExam.getId()).thenReturn(EXAM_ID);
        when(draftExam.getStatus()).thenReturn(ExamStatus.DRAFT);
        when(examRepository.findByIdIn(anyCollection())).thenReturn(List.of(draftExam));
        givenSchedule(ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(1800));

        List<ExamScheduleDto> result = useCase.execute(new ViewExamSchedulesQuery(null, null, null, null));

        assertThat(result).isEmpty();
    }

    @Test
    void should_hide_draft_and_moved_schedules() {
        givenSchedule(ExamScheduleStatus.DRAFT, NOW.plusSeconds(3600));
        givenSchedule(ExamScheduleStatus.MOVED, NOW.plusSeconds(7200));
        ExamSchedule published = givenSchedule(ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(1800));

        List<ExamScheduleDto> result = useCase.execute(new ViewExamSchedulesQuery(null, null, null, null));

        assertThat(result).extracting(dto -> dto.id()).containsExactly(published.getId());
    }

    @Test
    void should_keep_cancelled_schedule() {
        ExamSchedule cancelled = givenSchedule(ExamScheduleStatus.CANCELLED, NOW.plusSeconds(3600));

        List<ExamScheduleDto> result = useCase.execute(new ViewExamSchedulesQuery(null, null, null, null));

        assertThat(result).extracting(dto -> dto.id()).containsExactly(cancelled.getId());
    }

    @Test
    void should_sort_schedules_from_newest_to_oldest() {
        ExamSchedule oldest = givenSchedule(ExamScheduleStatus.COMPLETED, NOW.minusSeconds(86400));
        ExamSchedule newest = givenSchedule(ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(86400));
        ExamSchedule middle = givenSchedule(ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(3600));

        List<ExamScheduleDto> result = useCase.execute(new ViewExamSchedulesQuery(null, null, null, null));

        assertThat(result).extracting(dto -> dto.id())
            .containsExactly(newest.getId(), middle.getId(), oldest.getId());
    }

    /** The client's filter must not be used as a back door to get unpublished sessions. */
    @Test
    void should_ignore_draft_status_filter_from_client() {
        givenSchedule(ExamScheduleStatus.DRAFT, NOW.plusSeconds(3600));

        List<ExamScheduleDto> result = useCase.execute(new ViewExamSchedulesQuery(null, ExamScheduleStatus.DRAFT, null, null));

        assertThat(result).isEmpty();
    }

    private ExamSchedule givenSchedule(ExamScheduleStatus status, Instant startDate) {
        ExamSchedule schedule = new ExamSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setExamId(EXAM_ID);
        schedule.setStatus(status);
        schedule.setStartDate(startDate);
        schedule.setEndDate(startDate.plusSeconds(3600));
        schedules.add(schedule);

        ExamCandidate candidate = new ExamCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setExamId(EXAM_ID);
        candidate.setStudentId(STUDENT_ID);
        candidate.setScheduleId(schedule.getId());
        candidate.setStatus(ExamCandidateStatus.ASSIGNED);
        candidates.add(candidate);
        return schedule;
    }
}
