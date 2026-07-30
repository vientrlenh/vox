package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.query.ViewExamSchedulesQuery;
import com.sep.vox.application.port.input.usecase.examschedule.UpdateExamScheduleUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.ViewExamSchedulesUseCase;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.dto.ExamScheduleProctorDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.ExamScheduleProctorDtoMapper;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.interfaces.graphql.dto.request.UpdateExamScheduleInput;
import com.sep.vox.interfaces.graphql.mapper.UpdateExamScheduleCommandMapper;

@Controller("graphqlExamScheduleController")
public class ExamScheduleController {

    private final ViewExamSchedulesUseCase viewExamSchedulesUseCase;
    private final UpdateExamScheduleUseCase updateExamScheduleUseCase;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamCandidateRepository examCandidateRepository;

    public ExamScheduleController(
            ViewExamSchedulesUseCase viewExamSchedulesUseCase,
            UpdateExamScheduleUseCase updateExamScheduleUseCase,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamCandidateRepository examCandidateRepository) {
        this.viewExamSchedulesUseCase = viewExamSchedulesUseCase;
        this.updateExamScheduleUseCase = updateExamScheduleUseCase;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examCandidateRepository = examCandidateRepository;
    }

    @QueryMapping(name = "examSchedules")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public List<ExamScheduleDto> examSchedules(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "status") ExamScheduleStatus status,
            @Argument(name = "startDate") String startDate,
            @Argument(name = "endDate") String endDate) {
        return viewExamSchedulesUseCase.execute(
            new ViewExamSchedulesQuery(
                examId, 
                status, 
                DateMapper.toInstant(startDate), 
                DateMapper.toInstant(endDate)
            )
        );
    }

    @SchemaMapping(typeName = "ExamSchedule", field = "room")
    public CompletableFuture<SchoolRoomFromDto> room(ExamScheduleDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, SchoolRoomFromDto> loader = env.getDataLoader("schoolRoomById");
        return loader.load(source.schoolRoomId());
    }

    @SchemaMapping(typeName = "ExamSchedule", field = "proctors")
    public List<ExamScheduleProctorDto> proctors(ExamScheduleDto source) {
        return ExamScheduleProctorDtoMapper.toDtoList(examScheduleProctorRepository.findByScheduleId(source.id()));
    }

    @SchemaMapping(typeName = "ExamSchedule", field = "candidateCount")
    public int candidateCount(ExamScheduleDto source) {
        return (int) examCandidateRepository.countByScheduleId(source.id());
    }

    @SchemaMapping(typeName = "ExamSchedule", field = "requiredProctorCount")
    public int requiredProctorCount(ExamScheduleDto source) {
        return 1;
    }

    @SchemaMapping(typeName = "ExamScheduleProctor", field = "teacher")
    public CompletableFuture<UserDto> teacher(ExamScheduleProctorDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(source.teacherId());
    }

    @MutationMapping(name = "updateExamSchedule")
    public UUID updateExamSchedule(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateExamScheduleInput input) {
        return updateExamScheduleUseCase.execute(UpdateExamScheduleCommandMapper.fromRequest(id, input));
    }
}
