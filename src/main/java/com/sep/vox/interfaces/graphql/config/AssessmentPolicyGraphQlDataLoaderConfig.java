package com.sep.vox.interfaces.graphql.config;

import com.sep.vox.domain.dto.FrameworkResultBandDto;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.dto.RubricVersionDto;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeLevelDto;
import com.sep.vox.domain.mapper.FrameworkResultBandDtoMapper;
import com.sep.vox.domain.mapper.FrameworkVersionDtoMapper;
import com.sep.vox.domain.mapper.RubricVersionDtoMapper;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.mapper.SchoolDtoMapper;
import com.sep.vox.domain.mapper.SchoolGradeLevelDtoMapper;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
public class AssessmentPolicyGraphQlDataLoaderConfig {

    public AssessmentPolicyGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            SchoolRepository schoolRepository,
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolClassRepository schoolClassRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            RubricVersionRepository rubricVersionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository
    ) {
        registry.<UUID, SchoolDto>forName("schoolDataLoader")
                .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            if (ids.isEmpty()) return Collections.emptyMap();
                            return schoolRepository.findByIdIn(ids).stream()
                                    .map(SchoolDtoMapper::toSchoolDto)
                                    .collect(Collectors.toMap(SchoolDto::id, dto -> dto));
                        })
                );

        registry.<UUID, SchoolGradeLevelDto>forName("schoolGradeLevelDataLoader")
                .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            if (ids.isEmpty()) return Collections.emptyMap();
                            return schoolGradeLevelRepository.findByIdIn(ids).stream()
                                    .map(SchoolGradeLevelDtoMapper::toDto)
                                    .collect(Collectors.toMap(SchoolGradeLevelDto::id, dto -> dto));
                        })
                );

        registry.<UUID, SchoolClassDto>forName("schoolClassDataLoader")
                .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            if (ids.isEmpty()) return Collections.emptyMap();
                            return schoolClassRepository.findAllById(ids.stream().toList()).stream()
                                    .map(SchoolClassDtoMapper::toDto)
                                    .collect(Collectors.toMap(SchoolClassDto::id, dto -> dto));
                        })
                );

        registry.<UUID, FrameworkVersionDto>forName("frameworkVersionDataLoader")
                .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            if (ids.isEmpty()) return Collections.emptyMap();
                            return frameworkVersionRepository.findByIdIn(ids).stream()
                                    .map(FrameworkVersionDtoMapper::toDto)
                                    .collect(Collectors.toMap(FrameworkVersionDto::id, dto -> dto));
                        })
                );

        registry.<UUID, RubricVersionDto>forName("rubricVersionDataLoader")
                .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            if (ids.isEmpty()) return Collections.emptyMap();
                            return rubricVersionRepository.findByIdIn(ids).stream()
                                    .map(RubricVersionDtoMapper::toRubricVersionDto)
                                    .collect(Collectors.toMap(RubricVersionDto::id, dto -> dto));
                        })
                );

        registry.<UUID, FrameworkResultBandDto>forName("frameworkResultBandDataLoader")
                .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            if (ids.isEmpty()) return Collections.emptyMap();
                            return frameworkResultBandRepository.findAllByIds(ids.stream().toList()).stream()
                                    .map(FrameworkResultBandDtoMapper::toDto)
                                    .collect(Collectors.toMap(FrameworkResultBandDto::id, dto -> dto));
                        })
                );
    }
}
