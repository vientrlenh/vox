package com.sep.vox.interfaces.graphql.controller;

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

import com.sep.vox.application.port.input.usecase.examschedule.UpdateExamScheduleUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.ViewExamSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.ViewMyExamSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.ViewProctorBusySlotsUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.ViewStudentBusySlotsUseCase;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;

import graphql.schema.DataFetchingEnvironment;

class ExamScheduleControllerTests {

    private ViewExamSchedulesUseCase viewExamSchedulesUseCase;
    private ViewMyExamSchedulesUseCase viewMyExamSchedulesUseCase;
    private UpdateExamScheduleUseCase updateExamScheduleUseCase;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ViewStudentBusySlotsUseCase viewStudentBusySlotsUseCase;
    private ExamScheduleController controller;

    private final UUID examId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        viewExamSchedulesUseCase = mock(ViewExamSchedulesUseCase.class);
        viewMyExamSchedulesUseCase = mock(ViewMyExamSchedulesUseCase.class);
        updateExamScheduleUseCase = mock(UpdateExamScheduleUseCase.class);
        examScheduleProctorRepository = mock(ExamScheduleProctorRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        viewStudentBusySlotsUseCase = mock(ViewStudentBusySlotsUseCase.class);
        controller = new ExamScheduleController(viewExamSchedulesUseCase, viewMyExamSchedulesUseCase,
            updateExamScheduleUseCase, mock(ViewProctorBusySlotsUseCase.class),
            viewStudentBusySlotsUseCase,
            examScheduleProctorRepository, examCandidateRepository);
    }

    private ExamScheduleDto schedule(UUID schoolRoomId) {
        return new ExamScheduleDto(scheduleId, examId, schoolRoomId, null, null, "DRAFT", null);
    }

    private SchoolRoomFromDto room(UUID roomId) {
        return new SchoolRoomFromDto(roomId, UUID.randomUUID(), "R1", "Phòng 1", null, true,
            null, null, null, null);
    }

    @Test
    void should_return_null_room_when_schedule_has_no_room() {
        var dto = schedule(null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);

        var result = controller.room(dto, env);

        assertThat(result.join()).isNull();
        verify(env, never()).getDataLoader(any());
    }

    /**
     * Ca thi nháp của bài kiểm tra trên lớp được phép chưa có phòng (giáo viên chọn phòng sau). Nếu
     * resolver đẩy khoá null vào DataLoader thì java-dataloader ném NPE và cả danh sách ca thi hỏng.
     */
    @Test
    void should_not_pass_null_key_to_real_data_loader_when_schedule_has_no_room() {
        var dto = schedule(null);
        DataLoader<UUID, SchoolRoomFromDto> loader = DataLoaderFactory.newMappedDataLoader(
            (java.util.Set<UUID> keys, org.dataloader.BatchLoaderEnvironment env) ->
                CompletableFuture.completedFuture(Map.<UUID, SchoolRoomFromDto>of()));
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        when(env.<UUID, SchoolRoomFromDto>getDataLoader("schoolRoomById")).thenReturn(loader);

        var result = controller.room(dto, env);

        assertThat(result.join()).isNull();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_resolve_room_via_data_loader() {
        var roomId = UUID.randomUUID();
        var dto = schedule(roomId);
        var room = room(roomId);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, SchoolRoomFromDto>getDataLoader("schoolRoomById")).thenReturn(loader);
        when(loader.load(roomId)).thenReturn(CompletableFuture.completedFuture(room));

        var result = controller.room(dto, env);

        assertThat(result.join()).isSameAs(room);
        verify(loader).load(roomId);
    }

    @Test
    void should_delegate_exam_schedules_query_to_use_case() {
        var dto = schedule(UUID.randomUUID());
        when(viewExamSchedulesUseCase.execute(any())).thenReturn(List.of(dto));

        var result = controller.examSchedules(examId, null, null, null);

        assertThat(result).containsExactly(dto);
    }
}
