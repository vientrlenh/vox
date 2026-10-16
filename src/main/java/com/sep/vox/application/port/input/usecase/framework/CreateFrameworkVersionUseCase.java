package com.sep.vox.application.port.input.usecase.framework;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.framework.CreateFrameworkCriterionCommand;
import com.sep.vox.application.port.input.command.framework.CreateFrameworkResultBandCommand;
import com.sep.vox.application.port.input.command.framework.CreateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.framework.CreateFrameworkVersionResponse;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.Code;

@Service 
public class CreateFrameworkVersionUseCase implements IUseCase<CreateFrameworkVersionCommand, CreateFrameworkVersionResponse> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final UserContextPort userContextPort;

    public CreateFrameworkVersionUseCase(
        FrameworkRepository frameworkRepository, 
        FrameworkVersionRepository frameworkVersionRepository, 
        FrameworkCriterionRepository frameworkCriterionRepository, 
        FrameworkResultBandRepository frameworkResultBandRepository, 
        UserContextPort userContextPort
    ) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional 
    public CreateFrameworkVersionResponse execute(CreateFrameworkVersionCommand input) {
        CreateFrameworkVersionCommand command = normalize(input);
        UUID authenticatedUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        
        Framework framework = frameworkRepository.findByIdForUpdate(command.frameworkId())
            .orElseThrow(() -> new ResourceNotFoundException("Framework " + command.frameworkId() + " is not found"));
        if (frameworkVersionRepository.containsDraftInFrameworkId(command.frameworkId())) {
            throw new ResourceDuplicatedException("The framework is containing a draft version. Cannot create a new one");
        }
        FrameworkVersion frameworkVersion = validateAndBuildFrameworkVersion(authenticatedUserId, framework.getCode(), command);
        if (!command.criteria().isEmpty()) {
            validateAndBuildCriteria(command.criteria(), frameworkVersion.getId());
        }
        if (!command.resultBands().isEmpty()) {
            validateAndBuildResultBands(command.resultBands(), frameworkVersion.getId());
        }
        return CreateFrameworkVersionResponse.toResponse(frameworkVersion.getId());
    }
    

    private CreateFrameworkVersionCommand normalize(CreateFrameworkVersionCommand input) {
        return new CreateFrameworkVersionCommand(
            input.frameworkId(), 
            StringNormalization.trimAndCollapseSpaces(input.name()), 
            StringNormalization.trimAndCollapseSpaces(input.description()), 
            input.effectiveFrom(), 
            input.effectiveTo(), 
            input.criteria().stream()
                .map(c -> new CreateFrameworkCriterionCommand(
                    StringNormalization.normalizeCode(c.code()), 
                    StringNormalization.trimAndCollapseSpaces(c.name()), 
                    StringNormalization.trimAndCollapseSpaces(c.description()))
                ).toList(), 
            input.resultBands().stream()
                .map(b -> new CreateFrameworkResultBandCommand(
                    StringNormalization.normalizeCode(b.code()), 
                    StringNormalization.trimAndCollapseSpaces(b.label()), 
                    StringNormalization.trimAndCollapseSpaces(b.description()), 
                    b.order())
                ).toList()
        );
    }

    private FrameworkVersion validateAndBuildFrameworkVersion(UUID createdBy, Code frameworkCode, CreateFrameworkVersionCommand command) {
        validateEffectiveDateRange(command.effectiveFrom(), command.effectiveTo());
        long version = buildVersion(command.frameworkId());
        String code = buildCode(Code.valueOf(frameworkCode), version);
        FrameworkVersion newVersion = FrameworkVersion.createDraft(
            command.frameworkId(), 
            code, 
            command.name(), 
            command.description(), 
            version, 
            command.effectiveFrom(), 
            command.effectiveTo(), 
            createdBy
        );
        return frameworkVersionRepository.saveAndFlush(newVersion);
    }

    private void validateEffectiveDateRange(Instant effectiveFrom, Instant effectiveTo) {
        if (effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("Effective to date must be after effective from date if requested");
        }
    }

    private long buildVersion(UUID frameworkId) {
        long version = frameworkVersionRepository.findLatestNonDraftVersionNumberOnFrameworkId(frameworkId);
        return version + 1;
    }

    private String buildCode(String frameworkCode, long version) {
        return frameworkCode + "_" + version;
    }

    private void validateAndBuildCriteria(List<CreateFrameworkCriterionCommand> commands, UUID frameworkVersionId) {
        Set<String> existCodes = new HashSet<>();
        commands.stream()
            .forEach(c -> {
                if (existCodes.contains(c.code())) {
                    throw new IllegalArgumentException("The code " + c.code() + " is duplicated in criteria request");
                }
                existCodes.add(c.code());
            });
        List<FrameworkCriterion> criteria = commands.stream()
            .map(c -> FrameworkCriterion.create(frameworkVersionId, c.code(), c.name(), c.description())
        ).toList();
        frameworkCriterionRepository.saveAll(criteria);
    }

    private void validateAndBuildResultBands(List<CreateFrameworkResultBandCommand> commands, UUID frameworkVersionId) {
        Set<String> existCodes = new HashSet<>();
        Set<String> existLabels = new HashSet<>();
        Set<Integer> validOrders = new HashSet<>();
        Set<Integer> requestedOrders = new HashSet<>();
        int increment = 1;
        for (CreateFrameworkResultBandCommand command : commands) {
            if (existCodes.contains(command.code())) {
                throw new IllegalArgumentException("The code " + command.code() + " is duplicated in result bands request");
            }
            if (existLabels.contains(command.label())) {
                throw new IllegalArgumentException("The label " + command.label() + " is duplicated in result bands request");
            }
            existCodes.add(command.code());
            existLabels.add(command.label());
            validOrders.add(increment);
            increment++;
            requestedOrders.add(command.order());
        }
        if (!validOrders.equals(requestedOrders)) {
            throw new IllegalArgumentException("The orders from the requested result bands are not valid");
        }
        List<FrameworkResultBand> resultBands = commands.stream()
            .map(b -> FrameworkResultBand.create(frameworkVersionId, b.code(), b.label(), b.description(), b.order())
        ).toList();
        frameworkResultBandRepository.saveAll(resultBands);
    }
}
