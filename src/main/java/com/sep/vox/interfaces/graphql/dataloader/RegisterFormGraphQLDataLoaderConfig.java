package com.sep.vox.interfaces.graphql.dataloader;

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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class RegisterFormGraphQLDataLoaderConfig {

    public static final String REGISTER_FORM_DOCUMENTS_BY_REGISTER_FORM_ID = "registerFormDocumentsByRegisterFormId";

    private final BatchLoaderRegistry registry;
    private final RegisterFormDocumentRepository registerFormDocumentRepository;

    @PostConstruct
    void registerLoaders() {
        registerRegisterFormDocumentsByRegisterFormId();
    }

    private void registerRegisterFormDocumentsByRegisterFormId() {
        registry.<UUID, List<RegisterFormDocumentDto>>forName(REGISTER_FORM_DOCUMENTS_BY_REGISTER_FORM_ID)
            .registerMappedBatchLoader((Set<UUID> registerFormIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<RegisterFormDocumentDto>> documentsByRegisterFormId = registerFormDocumentRepository
                        .findByRegisterFormIdIn(registerFormIds).stream()
                        .map(RegisterFormDocumentDtoMapper::toRegisterFormDocumentDto)
                        .collect(Collectors.groupingBy(document -> document.registerFormId()));

                    Map<UUID, List<RegisterFormDocumentDto>> result = new HashMap<>();
                    registerFormIds.forEach(id -> result.put(id, documentsByRegisterFormId.getOrDefault(id, List.of())));
                    return result;
                })
            );
    }
}
