package com.sep.vox.interfaces.graphql.config.dataloader;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.repository.UserRepository;

import reactor.core.publisher.Mono;

@Configuration 
public class GraphQLDataLoaderConfig {
    
    public GraphQLDataLoaderConfig(
        BatchLoaderRegistry registry, 
        UserRepository userRepository
    ) {
        registry.<UUID, UserDto>forName("userByUserId")
            .registerMappedBatchLoader((Set<UUID> userIds, BatchLoaderEnvironment env) -> 
                Mono.fromSupplier(() -> userRepository.findByIdIn(userIds)
                    .stream()
                    .map(UserDto::toDto)
                    .collect(Collectors.toMap(u -> u.id(), Function.identity()))
            )
        );

        
    }
}
