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

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.dto.FrameworkCriterionBandDto;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.dto.FrameworkResultBandDto;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;

import reactor.core.publisher.Mono;

@Configuration
public class FrameworkGraphQlDataLoaderConfig {

    public FrameworkGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            JsonSerializationPort jsonSerializationPort) {

        registry.<UUID, List<FrameworkCriterionDto>>forName("criteriaByFrameworkVersion")
        .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<UUID, List<FrameworkCriterionDto>> result = new HashMap<>();
                versionIds.forEach(id -> result.put(id, List.of()));

                var criteria = frameworkCriterionRepository.findByFrameworkVersionIdIn(versionIds);

                var criterionIds = criteria.stream().map(FrameworkCriterion::getId).toList();
                var allBands = criterionIds.isEmpty()
                    ? List.<FrameworkCriterionBand>of()
                    : frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds);

                var bandsByCriterionId = allBands.stream()
                    .collect(Collectors.groupingBy(FrameworkCriterionBand::getFrameworkCriterionId));

                var criteriaByVersionId = criteria.stream()
                    .map(c -> toCriterionDto(c, bandsByCriterionId.getOrDefault(c.getId(), List.of()), jsonSerializationPort))
                    .collect(Collectors.groupingBy(FrameworkCriterionDto::frameworkVersionId));

                result.putAll(criteriaByVersionId);
                return result;
            })
        );

        registry.<UUID, List<FrameworkResultBandDto>>forName("resultBandsByFrameworkVersion")
        .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<UUID, List<FrameworkResultBandDto>> result = new HashMap<>();
                versionIds.forEach(id -> result.put(id, List.of()));

                var bandsByVersionId = frameworkResultBandRepository.findByFrameworkVersionIdIn(versionIds)
                    .stream()
                    .map(FrameworkGraphQlDataLoaderConfig::toResultBandDto)
                    .collect(Collectors.groupingBy(FrameworkResultBandDto::frameworkVersionId));

                result.putAll(bandsByVersionId);
                return result;
            })
        );
    }

    private static FrameworkCriterionDto toCriterionDto(
            FrameworkCriterion criterion,
            List<FrameworkCriterionBand> bands,
            JsonSerializationPort json) {
        var bandDtos = bands.stream()
            .map(b -> new FrameworkCriterionBandDto(
                b.getId(),
                b.getFrameworkCriterionId(),
                b.getFrameworkResultBandId(),
                b.getDescriptor(),
                json.toJson(b.getPositiveSignals()),
                json.toJson(b.getNegativeSignals())
            ))
            .toList();
        return new FrameworkCriterionDto(
            criterion.getId(),
            criterion.getFrameworkVersionId(),
            criterion.getCode(),
            criterion.getName(),
            criterion.getDescription(),
            bandDtos
        );
    }

    private static FrameworkResultBandDto toResultBandDto(FrameworkResultBand band) {
        return new FrameworkResultBandDto(
            band.getId(),
            band.getFrameworkVersionId(),
            band.getCode(),
            band.getLabel(),
            band.getDescription(),
            band.getScoreMin(),
            band.getScoreMax(),
            band.getOrder(),
            band.getStatus().name()
        );
    }
}
