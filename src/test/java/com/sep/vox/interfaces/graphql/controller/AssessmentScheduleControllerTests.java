package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.assessmentschedule.UpdateAssessmentScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewAssessmentSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewMyAssessmentSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewProctorBusySlotsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewStudentBusySlotsUseCase;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;

import graphql.schema.DataFetchingEnvironment;

class AssessmentScheduleControllerTests {

    private ViewAssessmentSchedulesUseCase viewAssessmentSchedulesUseCase;
    private ViewMyAssessmentSchedulesUseCase viewMyAssessmentSchedulesUseCase;
    private UpdateAssessmentScheduleUseCase updateAssessmentScheduleUseCase;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private ViewStudentBusySlotsUseCase viewStudentBusySlotsUseCase;
    private AssessmentScheduleController controller;

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        viewAssessmentSchedulesUseCase = mock(ViewAssessmentSchedulesUseCase.class);
        viewMyAssessmentSchedulesUseCase = mock(ViewMyAssessmentSchedulesUseCase.class);
        updateAssessmentScheduleUseCase = mock(UpdateAssessmentScheduleUseCase.class);
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        viewStudentBusySlotsUseCase = mock(ViewStudentBusySlotsUseCase.class);
        controller = new AssessmentScheduleController(viewAssessmentSchedulesUseCase, viewMyAssessmentSchedulesUseCase,
            updateAssessmentScheduleUseCase, mock(ViewProctorBusySlotsUseCase.class),
            viewStudentBusySlotsUseCase,
            assessmentScheduleProctorRepository, assessmentCandidateRepository);
    }

    private AssessmentScheduleDto schedule(UUID schoolRoomId) {
        return new AssessmentScheduleDto(scheduleId, assessmentId, schoolRoomId, null, null, AssessmentScheduleStatus.DRAFT, null);
    }

    private SchoolRoomFromDto room(UUID roomId) {
        return new SchoolRoomFromDto(roomId, UUID.randomUUID(), "R1", "Phòng 1", null, true,
            null, null, null, null);
    }

    @Test
    void should_return_null_room_when_schedule_has_no_room() {
        AssessmentScheduleDto dto = schedule(null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);

        CompletableFuture<SchoolRoomFromDto> result = controller.room(dto, env);

        assertThat(result.join()).isNull();
        verify(env, never()).getDataLoader(any());
    }

    /**
     * The draft session of a class test is allowed to have no room yet (the teacher picks the room
     * later). If the resolver pushes a null key into the DataLoader, java-dataloader throws an NPE
     * and the whole session list breaks.
     */
    @Test
    void should_not_pass_null_key_to_real_data_loader_when_schedule_has_no_room() {
        AssessmentScheduleDto dto = schedule(null);
        DataLoader<UUID, SchoolRoomFromDto> loader = DataLoaderFactory.newMappedDataLoader(
            (java.util.Set<UUID> keys, org.dataloader.BatchLoaderEnvironment env) ->
                CompletableFuture.completedFuture(Map.<UUID, SchoolRoomFromDto>of()));
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        when(env.<UUID, SchoolRoomFromDto>getDataLoader("schoolRoomById")).thenReturn(loader);

        CompletableFuture<SchoolRoomFromDto> result = controller.room(dto, env);

        assertThat(result.join()).isNull();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_resolve_room_via_data_loader() {
        UUID roomId = UUID.randomUUID();
        AssessmentScheduleDto dto = schedule(roomId);
        SchoolRoomFromDto room = room(roomId);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, SchoolRoomFromDto>getDataLoader("schoolRoomById")).thenReturn(loader);
        when(loader.load(roomId)).thenReturn(CompletableFuture.completedFuture(room));

        CompletableFuture<SchoolRoomFromDto> result = controller.room(dto, env);

        assertThat(result.join()).isSameAs(room);
        verify(loader).load(roomId);
    }

    @Test
    void should_delegate_assessment_schedules_query_to_use_case() {
        AssessmentScheduleDto dto = schedule(UUID.randomUUID());
        when(viewAssessmentSchedulesUseCase.execute(any())).thenReturn(List.of(dto));

        List<AssessmentScheduleDto> result = controller.assessmentSchedules(assessmentId, null, null, null);

        assertThat(result).containsExactly(dto);
    }
}
