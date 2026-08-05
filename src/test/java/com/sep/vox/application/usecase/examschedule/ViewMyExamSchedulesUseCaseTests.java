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
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
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

    private final List<ExamCandidate> candidates = new ArrayList<>();
    private final List<ExamSchedule> schedules = new ArrayList<>();

    @BeforeEach
    void setUp() {
        var examCandidateRepository = mock(ExamCandidateRepository.class);
        var examScheduleRepository = mock(ExamScheduleRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new ViewMyExamSchedulesUseCase(examCandidateRepository, examScheduleRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);
        when(examCandidateRepository.findByStudentId(STUDENT_ID)).thenReturn(candidates);
        when(examScheduleRepository.findByIdIn(anyCollection())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            return schedules.stream().filter(schedule -> ids.contains(schedule.getId())).toList();
        });
    }

    @Test
    void should_hide_draft_and_moved_schedules() {
        givenSchedule(ExamScheduleStatus.DRAFT, NOW.plusSeconds(3600));
        givenSchedule(ExamScheduleStatus.MOVED, NOW.plusSeconds(7200));
        var published = givenSchedule(ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(1800));

        var result = useCase.execute(new ViewExamSchedulesQuery(null, null, null, null));

        assertThat(result).extracting(dto -> dto.id()).containsExactly(published.getId());
    }

    @Test
    void should_keep_cancelled_schedule() {
        var cancelled = givenSchedule(ExamScheduleStatus.CANCELLED, NOW.plusSeconds(3600));

        var result = useCase.execute(new ViewExamSchedulesQuery(null, null, null, null));

        assertThat(result).extracting(dto -> dto.id()).containsExactly(cancelled.getId());
    }

    @Test
    void should_sort_schedules_from_newest_to_oldest() {
        var oldest = givenSchedule(ExamScheduleStatus.COMPLETED, NOW.minusSeconds(86400));
        var newest = givenSchedule(ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(86400));
        var middle = givenSchedule(ExamScheduleStatus.PUBLISHED, NOW.plusSeconds(3600));

        var result = useCase.execute(new ViewExamSchedulesQuery(null, null, null, null));

        assertThat(result).extracting(dto -> dto.id())
            .containsExactly(newest.getId(), middle.getId(), oldest.getId());
    }

    /** Bộ lọc của client không được dùng làm cửa hậu để lấy ca chưa publish. */
    @Test
    void should_ignore_draft_status_filter_from_client() {
        givenSchedule(ExamScheduleStatus.DRAFT, NOW.plusSeconds(3600));

        var result = useCase.execute(new ViewExamSchedulesQuery(null, ExamScheduleStatus.DRAFT, null, null));

        assertThat(result).isEmpty();
    }

    private ExamSchedule givenSchedule(ExamScheduleStatus status, Instant startDate) {
        var schedule = new ExamSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setExamId(EXAM_ID);
        schedule.setStatus(status);
        schedule.setStartDate(startDate);
        schedule.setEndDate(startDate.plusSeconds(3600));
        schedules.add(schedule);

        var candidate = new ExamCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setExamId(EXAM_ID);
        candidate.setStudentId(STUDENT_ID);
        candidate.setScheduleId(schedule.getId());
        candidate.setStatus(ExamCandidateStatus.ASSIGNED);
        candidates.add(candidate);
        return schedule;
    }
}
