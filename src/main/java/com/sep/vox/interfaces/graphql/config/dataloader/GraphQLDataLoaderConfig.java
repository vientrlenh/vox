package com.sep.vox.interfaces.graphql.config.dataloader;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

import reactor.core.publisher.Mono;

@Configuration 
public class GraphQLDataLoaderConfig {
    
    public GraphQLDataLoaderConfig(
        BatchLoaderRegistry registry, 
        UserRepository userRepository, 
        SchoolRepository schoolRepository, 
        SchoolClassRepository schoolClassRepository
    ) {
        registry.<UUID, UserDto>forName("userByUserId")
            .registerMappedBatchLoader((Set<UUID> userIds, BatchLoaderEnvironment env) -> 
                Mono.fromSupplier(() -> userRepository.findByIdIn(userIds)
                    .stream()
                    .map(UserDto::toDto)
                    .collect(Collectors.toMap(u -> u.id(), Function.identity()))
            )
        );

        registry.<UUID, SchoolDto>forName("schoolBySchoolId")
            .registerMappedBatchLoader((Set<UUID> schoolIds, BatchLoaderEnvironment env) -> 
                Mono.fromSupplier(() -> schoolRepository.findByIdIn(schoolIds)
                    .stream()
                    .map(SchoolDto::toDto)
                    .collect(Collectors.toMap(s -> s.id(), Function.identity()))
            )
        );
    }
}
