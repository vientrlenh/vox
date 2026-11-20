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

import com.sep.vox.application.port.input.query.ViewExamBlueprintDetailsQuery;
import com.sep.vox.application.port.input.query.ViewExamBlueprintsQuery;
import com.sep.vox.application.port.input.usecase.examblueprint.ViewExamBlueprintDetailsUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.ViewExamBlueprintsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.dto.ExamBlueprintSectionDto;
import com.sep.vox.domain.dto.ExamBlueprintSlotDto;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.ExamBlueprintSectionDtoMapper;
import com.sep.vox.domain.mapper.ExamBlueprintSlotDtoMapper;
import com.sep.vox.domain.mapper.ExamBlueprintVersionDtoMapper;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Controller("graphqlExamBlueprintController")
public class ExamBlueprintController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamBlueprintController.class);

    private final ViewExamBlueprintsUseCase viewExamBlueprintsUseCase;
    private final ViewExamBlueprintDetailsUseCase viewExamBlueprintDetailsUseCase;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final QuestionRepository questionRepository;

    public ExamBlueprintController(
            ViewExamBlueprintsUseCase viewExamBlueprintsUseCase,
            ViewExamBlueprintDetailsUseCase viewExamBlueprintDetailsUseCase,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            QuestionRepository questionRepository) {
        this.viewExamBlueprintsUseCase = viewExamBlueprintsUseCase;
        this.viewExamBlueprintDetailsUseCase = viewExamBlueprintDetailsUseCase;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.questionRepository = questionRepository;
    }

    @QueryMapping(name = "examBlueprints")
    public PageResult<ExamBlueprintDto> examBlueprints(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "isActive") Boolean isActive,
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "examKind") String examKind,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePage(page, size);
        return viewExamBlueprintsUseCase.execute(
            new ViewExamBlueprintsQuery(schoolId, isActive, languageId, examKind, keyword, page, size)
        );
    }

    @QueryMapping(name = "examBlueprint")
    public ExamBlueprintDto examBlueprint(@Argument(name = "id") UUID id) {
        var startedAt = System.nanoTime();
        LOGGER.info("[blueprint-perf] query examBlueprint start id={}", id);
        var dto = viewExamBlueprintDetailsUseCase.execute(new ViewExamBlueprintDetailsQuery(id));
        LOGGER.info("[blueprint-perf] query examBlueprint done id={} tookMs={}", id, (System.nanoTime() - startedAt) / 1_000_000);
        return dto;
    }

    @QueryMapping(name = "examBlueprintVersion")
    public ExamBlueprintVersionDto examBlueprintVersion(@Argument(name = "id") UUID id) {
        var startedAt = System.nanoTime();
        LOGGER.info("[blueprint-perf] query examBlueprintVersion start id={}", id);
        var dto = examBlueprintVersionRepository.findById(id)
            .map(version -> {
                viewExamBlueprintDetailsUseCase.execute(new ViewExamBlueprintDetailsQuery(version.getBlueprintId()));
                return ExamBlueprintVersionDtoMapper.toDto(version);
            })
            .orElse(null);
        LOGGER.info("[blueprint-perf] query examBlueprintVersion done id={} tookMs={}", id, (System.nanoTime() - startedAt) / 1_000_000);
        return dto;
    }

    @SchemaMapping(typeName = "ExamBlueprint", field = "versions")
    public List<ExamBlueprintVersionDto> versions(
            ExamBlueprintDto source,
            @Argument(name = "status") ExamBlueprintVersionStatus status) {
        var versions = examBlueprintVersionRepository.findByBlueprintId(source.id()).stream()
            .map(ExamBlueprintVersionDtoMapper::toDto)
            .toList();
        return status == null
            ? versions
            : versions.stream().filter(version -> status.name().equals(version.status())).toList();
    }

    @SchemaMapping(typeName = "ExamBlueprint", field = "versionCount")
    public int versionCount(ExamBlueprintDto source) {
        return examBlueprintVersionRepository.findByBlueprintId(source.id()).size();
    }

    @SchemaMapping(typeName = "ExamBlueprint", field = "currentVersion")
    public ExamBlueprintVersionDto currentVersion(ExamBlueprintDto source) {
        return latestVersion(examBlueprintVersionRepository.findByBlueprintId(source.id()).stream()
            .map(ExamBlueprintVersionDtoMapper::toDto)
            .toList());
    }

    @SchemaMapping(typeName = "ExamBlueprint", field = "sectionCount")
    public int blueprintSectionCount(ExamBlueprintDto source) {
        var current = currentVersion(source);
        return current == null
            ? 0
            : examBlueprintSectionRepository.findByBlueprintVersionId(current.id()).size();
    }

    @SchemaMapping(typeName = "ExamBlueprintVersion", field = "sections")
    public List<ExamBlueprintSectionDto> sections(ExamBlueprintVersionDto source) {
        return examBlueprintSectionRepository.findByBlueprintVersionId(source.id()).stream()
            .map(ExamBlueprintSectionDtoMapper::toDto)
            .toList();
    }

    @SchemaMapping(typeName = "ExamBlueprintVersion", field = "sectionCount")
    public int versionSectionCount(ExamBlueprintVersionDto source) {
        return examBlueprintSectionRepository.findByBlueprintVersionId(source.id()).size();
    }

    @SchemaMapping(typeName = "ExamBlueprintVersion", field = "slotCount")
    public int slotCount(ExamBlueprintVersionDto source) {
        return examBlueprintSlotRepository.findByBlueprintVersionId(source.id()).size();
    }

    @SchemaMapping(typeName = "ExamBlueprintVersion", field = "weightSum")
    public BigDecimal weightSum(ExamBlueprintVersionDto source) {
        return examBlueprintSectionRepository.findByBlueprintVersionId(source.id()).stream()
            .map(ExamBlueprintSectionDtoMapper::toDto)
            .map(section -> section.sectionWeight() == null ? BigDecimal.ZERO : section.sectionWeight())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }

    @SchemaMapping(typeName = "ExamBlueprintSection", field = "slots")
    public List<ExamBlueprintSlotDto> slots(ExamBlueprintSectionDto source) {
        return examBlueprintSlotRepository.findBySectionId(source.id()).stream()
            .map(ExamBlueprintSlotDtoMapper::toDto)
            .toList();
    }

    @SchemaMapping(typeName = "ExamBlueprintSlot", field = "fixedQuestion")
    public QuestionDto fixedQuestion(ExamBlueprintSlotDto source) {
        if (source.fixedQuestionId() == null) {
            return null;
        }
        return questionRepository.findById(source.fixedQuestionId())
            .map(QuestionDtoMapper::toQuestionDto)
            .orElse(null);
    }

    private static ExamBlueprintVersionDto latestVersion(List<ExamBlueprintVersionDto> versions) {
        return versions.stream().max(Comparator.comparingInt(blueprintVersion -> blueprintVersion.version())).orElse(null);
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || size == null || page < 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
    }
}
