package com.sep.vox.interfaces.graphql.controller;

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
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.dto.AssessmentBlueprintSectionDto;
import com.sep.vox.domain.dto.AssessmentBlueprintSlotDto;
import com.sep.vox.domain.dto.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.shared.PageResult;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor 
public class GraphQLAssessmentBlueprintController {

    private final ViewAssessmentBlueprintsUseCase viewAssessmentBlueprintsUseCase;
    private final ViewAssessmentBlueprintDetailsUseCase viewAssessmentBlueprintDetailsUseCase;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final QuestionRepository questionRepository;

    @QueryMapping(name = "assessmentBlueprints")
    public PageResult<AssessmentBlueprintDto> assessmentBlueprints(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "isActive") Boolean isActive,
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "assessmentKind") String assessmentKind,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePage(page, size);
        return viewAssessmentBlueprintsUseCase.execute(
            new ViewAssessmentBlueprintsQuery(schoolId, isActive, languageId, assessmentKind, keyword, page, size)
        );
    }

    @QueryMapping(name = "assessmentBlueprint")
    public AssessmentBlueprintDto assessmentBlueprint(@Argument(name = "id") UUID id) {
        var startedAt = System.nanoTime();
        LOGGER.info("[blueprint-perf] query assessmentBlueprint start id={}", id);
        var dto = viewAssessmentBlueprintDetailsUseCase.execute(new ViewAssessmentBlueprintDetailsQuery(id));
        LOGGER.info("[blueprint-perf] query assessmentBlueprint done id={} tookMs={}", id, (System.nanoTime() - startedAt) / 1_000_000);
        return dto;
    }

    @QueryMapping(name = "assessmentBlueprintVersion")
    public AssessmentBlueprintVersionDto assessmentBlueprintVersion(@Argument(name = "id") UUID id) {
        var startedAt = System.nanoTime();
        LOGGER.info("[blueprint-perf] query assessmentBlueprintVersion start id={}", id);
        var dto = assessmentBlueprintVersionRepository.findById(id)
            .map(version -> {
                viewAssessmentBlueprintDetailsUseCase.execute(new ViewAssessmentBlueprintDetailsQuery(version.getBlueprintId()));
                return AssessmentBlueprintVersionDto.toDto(version);
            })
            .orElse(null);
        LOGGER.info("[blueprint-perf] query assessmentBlueprintVersion done id={} tookMs={}", id, (System.nanoTime() - startedAt) / 1_000_000);
        return dto;
    }

    @SchemaMapping(typeName = "AssessmentBlueprint", field = "versions")
    public List<AssessmentBlueprintVersionDto> versions(
            AssessmentBlueprintDto source,
            @Argument(name = "status") AssessmentBlueprintVersionStatus status) {
        var versions = assessmentBlueprintVersionRepository.findByBlueprintId(source.id()).stream()
            .map(AssessmentBlueprintVersionDto::toDto)
            .toList();
        return status == null
            ? versions
            : versions.stream().filter(version -> status.name().equals(version.status())).toList();
    }

    @SchemaMapping(typeName = "AssessmentBlueprint", field = "versionCount")
    public int versionCount(AssessmentBlueprintDto source) {
        return assessmentBlueprintVersionRepository.findByBlueprintId(source.id()).size();
    }

    @SchemaMapping(typeName = "AssessmentBlueprint", field = "currentVersion")
    public AssessmentBlueprintVersionDto currentVersion(AssessmentBlueprintDto source) {
        return latestVersion(assessmentBlueprintVersionRepository.findByBlueprintId(source.id()).stream()
            .map(AssessmentBlueprintVersionDto::toDto)
            .toList());
    }

    @SchemaMapping(typeName = "AssessmentBlueprint", field = "sectionCount")
    public int blueprintSectionCount(AssessmentBlueprintDto source) {
        var current = currentVersion(source);
        return current == null
            ? 0
            : assessmentBlueprintSectionRepository.findByBlueprintVersionId(current.id()).size();
    }

    @SchemaMapping(typeName = "AssessmentBlueprintVersion", field = "sections")
    public List<AssessmentBlueprintSectionDto> sections(AssessmentBlueprintVersionDto source) {
        return assessmentBlueprintSectionRepository.findByBlueprintVersionId(source.id()).stream()
            .map(AssessmentBlueprintSectionDto::toDto)
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
            .map(AssessmentBlueprintSectionDto::toDto)
            .map(section -> section.sectionWeight() == null ? BigDecimal.ZERO : section.sectionWeight())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }

    @SchemaMapping(typeName = "AssessmentBlueprintSection", field = "slots")
    public List<AssessmentBlueprintSlotDto> slots(AssessmentBlueprintSectionDto source) {
        return assessmentBlueprintSlotRepository.findBySectionId(source.id()).stream()
            .map(AssessmentBlueprintSlotDto::toDto)
            .toList();
    }

    @SchemaMapping(typeName = "AssessmentBlueprintSlot", field = "fixedQuestion")
    public QuestionDto fixedQuestion(AssessmentBlueprintSlotDto source) {
        if (source.fixedQuestionId() == null) {
            return null;
        }
        return questionRepository.findById(source.fixedQuestionId())
            .map(QuestionDto::toDto)
            .orElse(null);
    }

    private static AssessmentBlueprintVersionDto latestVersion(List<AssessmentBlueprintVersionDto> versions) {
        return versions.stream().max(Comparator.comparingInt(blueprintVersion -> blueprintVersion.version())).orElse(null);
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || size == null || page < 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
    }
}
