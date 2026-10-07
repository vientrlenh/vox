package com.sep.vox.interfaces.graphql.config.dataloader;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.UserRepository;

import reactor.core.publisher.Mono;

@Configuration 
public class GraphQLDataLoaderConfig {
    
    public GraphQLDataLoaderConfig(
        BatchLoaderRegistry registry, 
        UserRepository userRepository
    ) {
        registry.forTypePair(UUID.class, User.class)
            .registerMappedBatchLoader((ids, env) -> 
                Mono.fromSupplier(() -> userRepository.findByIdIn(ids).stream()
                    .collect(Collectors.toMap(u -> u.getId(), Function.identity()))
            ));
    }
}
