package com.sep.vox.interfaces.graphql.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.ExamBlueprintSectionDto;
import com.sep.vox.domain.dto.ExamBlueprintSlotDto;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.mapper.ExamBlueprintSectionDtoMapper;
import com.sep.vox.domain.mapper.ExamBlueprintSlotDtoMapper;
import com.sep.vox.domain.mapper.ExamBlueprintVersionDtoMapper;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;

import reactor.core.publisher.Mono;

@Configuration
public class ExamBlueprintGraphQlDataLoaderConfig {

    public ExamBlueprintGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository) {

        registry.<UUID, List<ExamBlueprintVersionDto>>forName("examBlueprintVersionsByBlueprintId")
            .registerMappedBatchLoader((Set<UUID> blueprintIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<ExamBlueprintVersionDto>> result = new HashMap<>();
                    blueprintIds.forEach(id -> result.put(id, List.of()));
                    examBlueprintVersionRepository.findByBlueprintIdIn(blueprintIds).stream()
                        .map(ExamBlueprintVersionDtoMapper::toDto)
                        .collect(Collectors.groupingBy(ExamBlueprintVersionDto::blueprintId))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, List<ExamBlueprintSectionDto>>forName("examBlueprintSectionsByVersionId")
            .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<ExamBlueprintSectionDto>> result = new HashMap<>();
                    versionIds.forEach(id -> result.put(id, List.of()));
                    examBlueprintSectionRepository.findByBlueprintVersionIdIn(versionIds).stream()
                        .map(ExamBlueprintSectionDtoMapper::toDto)
                        .collect(Collectors.groupingBy(ExamBlueprintSectionDto::blueprintVersionId))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, List<ExamBlueprintSlotDto>>forName("examBlueprintSlotsByVersionId")
            .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<ExamBlueprintSlotDto>> result = new HashMap<>();
                    versionIds.forEach(id -> result.put(id, List.of()));
                    examBlueprintSlotRepository.findByBlueprintVersionIdIn(versionIds).stream()
                        .map(ExamBlueprintSlotDtoMapper::toDto)
                        .collect(Collectors.groupingBy(ExamBlueprintSlotDto::blueprintVersionId))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, List<ExamBlueprintSlotDto>>forName("examBlueprintSlotsBySectionId")
            .registerMappedBatchLoader((Set<UUID> sectionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<ExamBlueprintSlotDto>> result = new HashMap<>();
                    sectionIds.forEach(id -> result.put(id, List.of()));
                    examBlueprintSlotRepository.findBySectionIdIn(sectionIds).stream()
                        .map(ExamBlueprintSlotDtoMapper::toDto)
                        .collect(Collectors.groupingBy(ExamBlueprintSlotDto::sectionId))
                        .forEach(result::put);
                    return result;
                })
            );
    }
}
