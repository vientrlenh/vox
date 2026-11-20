package com.sep.vox.application.usecase.assessmentschedule;

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

import com.sep.vox.application.port.input.query.ViewAssessmentSchedulesQuery;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewMyAssessmentSchedulesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;

/**
 * Lịch thi của học sinh trước đây chỉ loại ca DELETED, nên ca DRAFT (chưa publish) và ca đã dời vẫn
 * hiện, lại không sắp xếp gì cả.
 */
class ViewMyAssessmentSchedulesUseCaseTests {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private ViewMyAssessmentSchedulesUseCase useCase;
    private AssessmentRepository assessmentRepository;

    private final List<AssessmentCandidate> candidates = new ArrayList<>();
    private final List<AssessmentSchedule> schedules = new ArrayList<>();

    @BeforeEach
    void setUp() {
        AssessmentCandidateRepository assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        AssessmentScheduleRepository assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        UserContextPort userContextPort = mock(UserContextPort.class);
        assessmentRepository = mock(AssessmentRepository.class);

        useCase = new ViewMyAssessmentSchedulesUseCase(
            assessmentCandidateRepository, assessmentScheduleRepository, assessmentRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);
        when(assessmentCandidateRepository.findByStudentId(STUDENT_ID)).thenReturn(candidates);
        // A session only shows when the assessment has been published; every session in this test set
        // belongs to a SCHEDULED assessment so that the cases test exactly what they mean to test, the
        // SESSION status.
        Assessment assessment = mock(Assessment.class);
        when(assessment.getId()).thenReturn(ASSESSMENT_ID);
        when(assessment.getStatus()).thenReturn(AssessmentStatus.SCHEDULED);
        when(assessmentRepository.findByIdIn(anyCollection())).thenReturn(List.of(assessment));
        when(assessmentScheduleRepository.findByIdIn(anyCollection())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            return schedules.stream().filter(schedule -> ids.contains(schedule.getId())).toList();
        });
    }

    /**
     * A session that is published but whose ASSESSMENT is still DRAFT must still be hidden: the two
     * statuses are independent, and the schedule of an unpublished assessment must not be exposed to
     * candidates.
     */
    @Test
    void should_hide_schedules_of_draft_assessment() {
        Assessment draftAssessment = mock(Assessment.class);
        when(draftAssessment.getId()).thenReturn(ASSESSMENT_ID);
        when(draftAssessment.getStatus()).thenReturn(AssessmentStatus.DRAFT);
        when(assessmentRepository.findByIdIn(anyCollection())).thenReturn(List.of(draftAssessment));
        givenSchedule(AssessmentScheduleStatus.PUBLISHED, NOW.plusSeconds(1800));

        List<AssessmentScheduleDto> result = useCase.execute(new ViewAssessmentSchedulesQuery(null, null, null, null));

        assertThat(result).isEmpty();
    }

    @Test
    void should_hide_draft_and_moved_schedules() {
        givenSchedule(AssessmentScheduleStatus.DRAFT, NOW.plusSeconds(3600));
        givenSchedule(AssessmentScheduleStatus.MOVED, NOW.plusSeconds(7200));
        AssessmentSchedule published = givenSchedule(AssessmentScheduleStatus.PUBLISHED, NOW.plusSeconds(1800));

        List<AssessmentScheduleDto> result = useCase.execute(new ViewAssessmentSchedulesQuery(null, null, null, null));

        assertThat(result).extracting(dto -> dto.id()).containsExactly(published.getId());
    }

    @Test
    void should_keep_cancelled_schedule() {
        AssessmentSchedule cancelled = givenSchedule(AssessmentScheduleStatus.CANCELLED, NOW.plusSeconds(3600));

        List<AssessmentScheduleDto> result = useCase.execute(new ViewAssessmentSchedulesQuery(null, null, null, null));

        assertThat(result).extracting(dto -> dto.id()).containsExactly(cancelled.getId());
    }

    @Test
    void should_sort_schedules_from_newest_to_oldest() {
        AssessmentSchedule oldest = givenSchedule(AssessmentScheduleStatus.COMPLETED, NOW.minusSeconds(86400));
        AssessmentSchedule newest = givenSchedule(AssessmentScheduleStatus.PUBLISHED, NOW.plusSeconds(86400));
        AssessmentSchedule middle = givenSchedule(AssessmentScheduleStatus.PUBLISHED, NOW.plusSeconds(3600));

        List<AssessmentScheduleDto> result = useCase.execute(new ViewAssessmentSchedulesQuery(null, null, null, null));

        assertThat(result).extracting(dto -> dto.id())
            .containsExactly(newest.getId(), middle.getId(), oldest.getId());
    }

    /** The client's filter must not be used as a back door to get unpublished sessions. */
    @Test
    void should_ignore_draft_status_filter_from_client() {
        givenSchedule(AssessmentScheduleStatus.DRAFT, NOW.plusSeconds(3600));

        List<AssessmentScheduleDto> result = useCase.execute(new ViewAssessmentSchedulesQuery(null, AssessmentScheduleStatus.DRAFT, null, null));

        assertThat(result).isEmpty();
    }

    private AssessmentSchedule givenSchedule(AssessmentScheduleStatus status, Instant startDate) {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setAssessmentId(ASSESSMENT_ID);
        schedule.setStatus(status);
        schedule.setStartDate(startDate);
        schedule.setEndDate(startDate.plusSeconds(3600));
        schedules.add(schedule);

        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setAssessmentId(ASSESSMENT_ID);
        candidate.setStudentId(STUDENT_ID);
        candidate.setScheduleId(schedule.getId());
        candidate.setStatus(AssessmentCandidateStatus.ASSIGNED);
        candidates.add(candidate);
        return schedule;
    }
}
