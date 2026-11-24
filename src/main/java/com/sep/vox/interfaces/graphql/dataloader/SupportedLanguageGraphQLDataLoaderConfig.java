package com.sep.vox.interfaces.graphql.dataloader;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.mapper.SupportedLanguageDtoMapper;
import com.sep.vox.domain.repository.SupportedLanguageRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class SupportedLanguageGraphQLDataLoaderConfig {

    public static final String SUPPORTED_LANGUAGE_BY_SUPPORTED_LANGUAGE_ID = "supportedLanguageBySupportedLanguageId";

    private final BatchLoaderRegistry registry;
    private final SupportedLanguageRepository supportedLanguageRepository;

    @PostConstruct
    void registerLoaders() {
        registerSupportedLanguageBySupportedLanguageId();
    }

    private void registerSupportedLanguageBySupportedLanguageId() {
        registry.<UUID, SupportedLanguageDto>forName(SUPPORTED_LANGUAGE_BY_SUPPORTED_LANGUAGE_ID)
            .registerMappedBatchLoader((Set<UUID> languageIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> supportedLanguageRepository.findByIdIn(languageIds)
                    .stream()
                    .map(SupportedLanguageDtoMapper::toDto)
                    .collect(Collectors.toMap(language -> language.id(), language -> language)))
            );
    }
}
