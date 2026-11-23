package com.sep.vox.application.usecase.assessmentschedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.assessmentschedule.CreateAssessmentScheduleCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.usecase.assessmentschedule.CreateAssessmentScheduleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class CreateAssessmentScheduleUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private SchoolRoomRepository schoolRoomRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private CreateAssessmentScheduleUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-07-10T08:00:00+07:00");
    private final Instant end = Instant.parse("2026-07-10T10:00:00+07:00");

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        schoolRoomRepository = mock(SchoolRoomRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        // Validator thật chạy trên hai repository đã mock: luật kiểm tra phòng vẫn được test đúng
        // như trước khi nó được tách ra khỏi use case.
        useCase = new CreateAssessmentScheduleUseCase(
            assessmentRepository, assessmentScheduleRepository,
            new AssessmentScheduleRoomValidator(schoolRoomRepository, assessmentScheduleRepository),
            assessmentMemberRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment()));
    }

    @Test
    void should_create_draft_schedule_on_happy_path() {
        SchoolRoom room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(assessmentScheduleRepository.existsOverlapping(roomId, start, end, null)).thenReturn(false);
        when(assessmentScheduleRepository.save(any(AssessmentSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        AssessmentScheduleDto result = useCase.execute(new CreateAssessmentScheduleCommand(assessmentId, roomId, start, end));

        assertThat(result.status()).isEqualTo(AssessmentScheduleStatus.DRAFT);
        assertThat(result.schoolRoomId()).isEqualTo(roomId);
        verify(assessmentScheduleRepository).save(any(AssessmentSchedule.class));
    }

    @Test
    void should_reject_when_end_not_after_start() {
        assertThatThrownBy(() -> useCase.execute(new CreateAssessmentScheduleCommand(assessmentId, roomId, end, start)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_when_room_belongs_to_other_school() {
        SchoolRoom room = room(UUID.randomUUID());
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> useCase.execute(new CreateAssessmentScheduleCommand(assessmentId, roomId, start, end)))
            .isInstanceOf(ForbiddenException.class);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_when_overlapping() {
        SchoolRoom room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(assessmentScheduleRepository.existsOverlapping(eq(roomId), eq(start), eq(end), eq(null))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new CreateAssessmentScheduleCommand(assessmentId, roomId, start, end)))
            .isInstanceOf(ResourceDuplicatedException.class);
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_when_window_shorter_than_assessment_time_duration() {
        Assessment assessment = assessment();
        // The session lasts 2 hours (start..end) but the assessment duration is 3 hours.
        assessment.setAssessmentTimeDurationSecond(3 * 3600);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        SchoolRoom room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> useCase.execute(new CreateAssessmentScheduleCommand(assessmentId, roomId, start, end)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("thời gian làm bài");
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_accept_when_window_equals_assessment_time_duration() {
        Assessment assessment = assessment();
        assessment.setAssessmentTimeDurationSecond(2 * 3600);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        SchoolRoom room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(assessmentScheduleRepository.existsOverlapping(roomId, start, end, null)).thenReturn(false);
        when(assessmentScheduleRepository.save(any(AssessmentSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        AssessmentScheduleDto result = useCase.execute(new CreateAssessmentScheduleCommand(assessmentId, roomId, start, end));

        assertThat(result.status()).isEqualTo(AssessmentScheduleStatus.DRAFT);
        verify(assessmentScheduleRepository).save(any(AssessmentSchedule.class));
    }

    @Test
    void should_reject_when_schedule_starts_before_assessment_open() {
        Assessment assessment = assessment();
        assessment.setOpenAt(start.plus(1, ChronoUnit.MINUTES));
        assessment.setCloseAt(end);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        SchoolRoom room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> useCase.execute(new CreateAssessmentScheduleCommand(assessmentId, roomId, start, end)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mở và đóng");
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_reject_when_schedule_ends_after_assessment_close() {
        Assessment assessment = assessment();
        assessment.setOpenAt(start);
        assessment.setCloseAt(end.minus(1, ChronoUnit.MINUTES));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        SchoolRoom room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> useCase.execute(new CreateAssessmentScheduleCommand(assessmentId, roomId, start, end)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mở và đóng");
        verify(assessmentScheduleRepository, never()).save(any());
    }

    @Test
    void should_accept_when_schedule_exactly_fills_assessment_window() {
        Assessment assessment = assessment();
        assessment.setOpenAt(start);
        assessment.setCloseAt(end);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        SchoolRoom room = room(schoolId);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(assessmentScheduleRepository.existsOverlapping(roomId, start, end, null)).thenReturn(false);
        when(assessmentScheduleRepository.save(any(AssessmentSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        AssessmentScheduleDto result = useCase.execute(new CreateAssessmentScheduleCommand(assessmentId, roomId, start, end));

        assertThat(result.status()).isEqualTo(AssessmentScheduleStatus.DRAFT);
        verify(assessmentScheduleRepository).save(any(AssessmentSchedule.class));
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }

    private SchoolRoom room(UUID ownerSchoolId) {
        SchoolRoom room = mock(SchoolRoom.class);
        when(room.getSchoolId()).thenReturn(ownerSchoolId);
        return room;
    }
}
