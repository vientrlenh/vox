package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.domain.model.assessment.AssessmentKind;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewAssessmentBlueprintDetailsQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentBlueprintsQuery;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.ViewAssessmentBlueprintDetailsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.ViewAssessmentBlueprintsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.dto.AssessmentBlueprintSectionDto;
import com.sep.vox.domain.dto.AssessmentBlueprintSlotDto;
import com.sep.vox.domain.dto.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintSectionDtoMapper;
import com.sep.vox.domain.mapper.AssessmentBlueprintSlotDtoMapper;
import com.sep.vox.domain.mapper.AssessmentBlueprintVersionDtoMapper;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.interfaces.shared.PageArguments;

@Controller("graphqlAssessmentBlueprintController")
public class AssessmentBlueprintController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssessmentBlueprintController.class);

    private final ViewAssessmentBlueprintsUseCase viewAssessmentBlueprintsUseCase;
    private final ViewAssessmentBlueprintDetailsUseCase viewAssessmentBlueprintDetailsUseCase;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final QuestionRepository questionRepository;

    public AssessmentBlueprintController(
            ViewAssessmentBlueprintsUseCase viewAssessmentBlueprintsUseCase,
            ViewAssessmentBlueprintDetailsUseCase viewAssessmentBlueprintDetailsUseCase,
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository,
            AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository,
            QuestionRepository questionRepository) {
        this.viewAssessmentBlueprintsUseCase = viewAssessmentBlueprintsUseCase;
        this.viewAssessmentBlueprintDetailsUseCase = viewAssessmentBlueprintDetailsUseCase;
        this.assessmentBlueprintVersionRepository = assessmentBlueprintVersionRepository;
        this.assessmentBlueprintSectionRepository = assessmentBlueprintSectionRepository;
        this.assessmentBlueprintSlotRepository = assessmentBlueprintSlotRepository;
        this.questionRepository = questionRepository;
    }

    @QueryMapping(name = "assessmentBlueprints")
    public PageResult<AssessmentBlueprintDto> assessmentBlueprints(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "isActive") Boolean isActive,
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "assessmentKind") AssessmentKind assessmentKind,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewAssessmentBlueprintsUseCase.execute(
            new ViewAssessmentBlueprintsQuery(schoolId, isActive, languageId, assessmentKind, keyword, page, size)
        );
    }

    @QueryMapping(name = "assessmentBlueprint")
    public AssessmentBlueprintDto assessmentBlueprint(@Argument(name = "id") UUID id) {
        long startedAt = System.nanoTime();
        LOGGER.info("[blueprint-perf] query assessmentBlueprint start id={}", id);
        AssessmentBlueprintDto dto = viewAssessmentBlueprintDetailsUseCase.execute(new ViewAssessmentBlueprintDetailsQuery(id));
        LOGGER.info("[blueprint-perf] query assessmentBlueprint done id={} tookMs={}", id, (System.nanoTime() - startedAt) / 1_000_000);
        return dto;
    }

    @QueryMapping(name = "assessmentBlueprintVersion")
    public AssessmentBlueprintVersionDto assessmentBlueprintVersion(@Argument(name = "id") UUID id) {
        long startedAt = System.nanoTime();
        LOGGER.info("[blueprint-perf] query assessmentBlueprintVersion start id={}", id);
        AssessmentBlueprintVersionDto dto = assessmentBlueprintVersionRepository.findById(id)
            .map(version -> {
                viewAssessmentBlueprintDetailsUseCase.execute(new ViewAssessmentBlueprintDetailsQuery(version.getBlueprintId()));
                return AssessmentBlueprintVersionDtoMapper.toDto(version);
            })
            .orElse(null);
        LOGGER.info("[blueprint-perf] query assessmentBlueprintVersion done id={} tookMs={}", id, (System.nanoTime() - startedAt) / 1_000_000);
        return dto;
    }

    @SchemaMapping(typeName = "AssessmentBlueprint", field = "versions")
    public List<AssessmentBlueprintVersionDto> versions(
            AssessmentBlueprintDto source,
            @Argument(name = "status") AssessmentBlueprintVersionStatus status) {
        List<AssessmentBlueprintVersionDto> versions = assessmentBlueprintVersionRepository.findByBlueprintId(source.id()).stream()
            .map(AssessmentBlueprintVersionDtoMapper::toDto)
            .toList();
        return status == null
            ? versions
            : versions.stream().filter(version -> version.status() == status).toList();
    }

    @SchemaMapping(typeName = "AssessmentBlueprint", field = "versionCount")
    public int versionCount(AssessmentBlueprintDto source) {
        return assessmentBlueprintVersionRepository.findByBlueprintId(source.id()).size();
    }

    @SchemaMapping(typeName = "AssessmentBlueprint", field = "currentVersion")
    public AssessmentBlueprintVersionDto currentVersion(AssessmentBlueprintDto source) {
        return latestVersion(assessmentBlueprintVersionRepository.findByBlueprintId(source.id()).stream()
            .map(AssessmentBlueprintVersionDtoMapper::toDto)
            .toList());
    }

    @SchemaMapping(typeName = "AssessmentBlueprint", field = "sectionCount")
    public int blueprintSectionCount(AssessmentBlueprintDto source) {
        AssessmentBlueprintVersionDto current = currentVersion(source);
        return current == null
            ? 0
            : assessmentBlueprintSectionRepository.findByBlueprintVersionId(current.id()).size();
    }

    @SchemaMapping(typeName = "AssessmentBlueprintVersion", field = "sections")
    public List<AssessmentBlueprintSectionDto> sections(AssessmentBlueprintVersionDto source) {
        return assessmentBlueprintSectionRepository.findByBlueprintVersionId(source.id()).stream()
            .map(AssessmentBlueprintSectionDtoMapper::toDto)
            .toList();
    }

    @SchemaMapping(typeName = "AssessmentBlueprintVersion", field = "sectionCount")
    public int versionSectionCount(AssessmentBlueprintVersionDto source) {
        return assessmentBlueprintSectionRepository.findByBlueprintVersionId(source.id()).size();
    }

    @SchemaMapping(typeName = "AssessmentBlueprintVersion", field = "slotCount")
    public int slotCount(AssessmentBlueprintVersionDto source) {
        return assessmentBlueprintSlotRepository.findByBlueprintVersionId(source.id()).size();
    }

    @SchemaMapping(typeName = "AssessmentBlueprintVersion", field = "weightSum")
    public BigDecimal weightSum(AssessmentBlueprintVersionDto source) {
        return assessmentBlueprintSectionRepository.findByBlueprintVersionId(source.id()).stream()
            .map(AssessmentBlueprintSectionDtoMapper::toDto)
            .map(section -> section.sectionWeight() == null ? BigDecimal.ZERO : section.sectionWeight())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }

    @SchemaMapping(typeName = "AssessmentBlueprintSection", field = "slots")
    public List<AssessmentBlueprintSlotDto> slots(AssessmentBlueprintSectionDto source) {
        return assessmentBlueprintSlotRepository.findBySectionId(source.id()).stream()
            .map(AssessmentBlueprintSlotDtoMapper::toDto)
            .toList();
    }

    @SchemaMapping(typeName = "AssessmentBlueprintSlot", field = "fixedQuestion")
    public QuestionDto fixedQuestion(AssessmentBlueprintSlotDto source) {
        if (source.fixedQuestionId() == null) {
            return null;
        }
        return questionRepository.findById(source.fixedQuestionId())
            .map(QuestionDtoMapper::toQuestionDto)
            .orElse(null);
    }

    private static AssessmentBlueprintVersionDto latestVersion(List<AssessmentBlueprintVersionDto> versions) {
        return versions.stream().max(Comparator.comparingInt(blueprintVersion -> blueprintVersion.version())).orElse(null);
    }
}
