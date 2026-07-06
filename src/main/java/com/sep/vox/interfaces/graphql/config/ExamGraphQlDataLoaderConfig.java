package com.sep.vox.interfaces.graphql.config;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.mapper.SchoolRoomDtoMapper;
import com.sep.vox.domain.repository.SchoolRoomRepository;

import reactor.core.publisher.Mono;

@Configuration
public class ExamGraphQlDataLoaderConfig {

    public ExamGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            SchoolRoomRepository schoolRoomRepository) {

        registry.<UUID, SchoolRoomFromDto>forName("schoolRoomById")
            .registerMappedBatchLoader((Set<UUID> roomIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> schoolRoomRepository.findByIdIn(roomIds)
                    .stream()
                    .map(SchoolRoomDtoMapper::toDto)
                    .collect(Collectors.toMap(SchoolRoomFromDto::id, room -> room)))
            );
    }
}
