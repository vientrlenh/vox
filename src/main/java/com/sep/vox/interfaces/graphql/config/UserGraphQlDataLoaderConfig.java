package com.sep.vox.interfaces.graphql.config;


import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.repository.UserRepository;

import reactor.core.publisher.Mono;

@Configuration
public class UserGraphQlDataLoaderConfig {

    public UserGraphQlDataLoaderConfig(
        BatchLoaderRegistry registry,
        UserRepository userRepository
    ) {

        registry.<UUID, UserDto>forName("userById")
        .registerMappedBatchLoader((Set<UUID> userIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> userRepository.findByIdIn(userIds)
                .stream()
                .map(UserDto::toDto)
                .collect(Collectors.toMap(user -> user.id(), user -> user)))
        );
    }
}
