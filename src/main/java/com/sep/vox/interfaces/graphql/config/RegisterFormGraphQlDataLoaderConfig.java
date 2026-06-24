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

import com.sep.vox.domain.dto.RegisterFormDocumentDto;
import com.sep.vox.domain.mapper.RegisterFormDocumentDtoMapper;
import com.sep.vox.domain.repository.RegisterFormDocumentRepository;

import reactor.core.publisher.Mono;

@Configuration
public class RegisterFormGraphQlDataLoaderConfig {
    
    public RegisterFormGraphQlDataLoaderConfig(
        BatchLoaderRegistry registry, 
        RegisterFormDocumentRepository registerFormDocumentRepository
    ) {
        registry.<UUID, List<RegisterFormDocumentDto>>forName("documentsByRegisterForm").registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) -> Mono.fromSupplier(() -> {
            Map<UUID, List<RegisterFormDocumentDto>> result = new HashMap<>();

            var documentsByRegisterFormId = registerFormDocumentRepository.findByRegisterFormIdIn(ids)
                .stream()
                .map(RegisterFormDocumentDtoMapper::toRegisterFormDocumentDto)
                .collect(Collectors.groupingBy(RegisterFormDocumentDto::registerFormId));

            ids.forEach(id -> result.put(id, documentsByRegisterFormId.getOrDefault(id, List.of())));
            return result;
        }
    ));
    }
}
