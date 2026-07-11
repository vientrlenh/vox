package com.sep.vox.interfaces.graphql.config;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.application.port.input.usecase.examevaluation.ResolveExamCandidateAttemptsUseCase;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.mapper.ExamPaperDtoMapper;
import com.sep.vox.domain.mapper.ExamScheduleDtoMapper;
import com.sep.vox.domain.mapper.SchoolRoomDtoMapper;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;

import reactor.core.publisher.Mono;

@Configuration
public class ExamGraphQlDataLoaderConfig {

    public ExamGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            SchoolRoomRepository schoolRoomRepository,
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamPaperRepository examPaperRepository,
            ResolveExamCandidateAttemptsUseCase resolveExamCandidateAttemptsUseCase) {

        registry.<UUID, SchoolRoomFromDto>forName("schoolRoomById")
            .registerMappedBatchLoader((Set<UUID> roomIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> schoolRoomRepository.findByIdIn(roomIds)
                    .stream()
                    .map(SchoolRoomDtoMapper::toDto)
                    .collect(Collectors.toMap(SchoolRoomFromDto::id, room -> room)))
            );

        registry.<UUID, ExamDto>forName("examById")
            .registerMappedBatchLoader((Set<UUID> examIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> examRepository.findByIdIn(examIds)
                    .stream()
                    .map(ExamDtoMapper::toDto)
                    .collect(Collectors.toMap(ExamDto::id, exam -> exam)))
            );

        registry.<UUID, ExamScheduleDto>forName("examScheduleById")
            .registerMappedBatchLoader((Set<UUID> scheduleIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> examScheduleRepository.findByIdIn(scheduleIds)
                    .stream()
                    .map(ExamScheduleDtoMapper::toDto)
                    .collect(Collectors.toMap(ExamScheduleDto::id, schedule -> schedule)))
            );

        registry.<UUID, ExamPaperDto>forName("examPaperById")
            .registerMappedBatchLoader((Set<UUID> paperIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> examPaperRepository.findByIdIn(paperIds)
                    .stream()
                    .map(ExamPaperDtoMapper::toDto)
                    .collect(Collectors.toMap(ExamPaperDto::id, paper -> paper)))
            );

        registry.<UUID, ResolveExamCandidateAttemptsUseCase.ExamCandidateAttempts>forName("examCandidateAttemptsByCandidateId")
            .registerMappedBatchLoader((Set<UUID> candidateIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> resolveExamCandidateAttemptsUseCase.executeBatch(candidateIds))
            );
    }
}
