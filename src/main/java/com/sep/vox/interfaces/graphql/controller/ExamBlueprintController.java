package com.sep.vox.interfaces.graphql.controller;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

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
import com.sep.vox.domain.mapper.ExamBlueprintVersionDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;

@Controller("graphqlExamBlueprintController")
public class ExamBlueprintController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamBlueprintController.class);

    private final ViewExamBlueprintsUseCase viewExamBlueprintsUseCase;
    private final ViewExamBlueprintDetailsUseCase viewExamBlueprintDetailsUseCase;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;

    public ExamBlueprintController(
            ViewExamBlueprintsUseCase viewExamBlueprintsUseCase,
            ViewExamBlueprintDetailsUseCase viewExamBlueprintDetailsUseCase,
            ExamBlueprintVersionRepository examBlueprintVersionRepository) {
        this.viewExamBlueprintsUseCase = viewExamBlueprintsUseCase;
        this.viewExamBlueprintDetailsUseCase = viewExamBlueprintDetailsUseCase;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
    }

    @QueryMapping(name = "examBlueprints")
    public PageResult<ExamBlueprintDto> examBlueprints(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "isActive") Boolean isActive,
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "examKind") String examKind,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
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
    public CompletableFuture<List<ExamBlueprintVersionDto>> versions(
            ExamBlueprintDto source,
            @Argument ExamBlueprintVersionStatus status,
            DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamBlueprintVersionDto>> loader = env.getDataLoader("examBlueprintVersionsByBlueprintId");
        return loader.load(source.id()).thenApply(versions -> status == null
            ? versions
            : versions.stream().filter(version -> status.name().equals(version.status())).toList());
    }

    @SchemaMapping(typeName = "ExamBlueprint", field = "versionCount")
    public CompletableFuture<Integer> versionCount(ExamBlueprintDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamBlueprintVersionDto>> loader = env.getDataLoader("examBlueprintVersionsByBlueprintId");
        return loader.load(source.id()).thenApply(List::size);
    }

    @SchemaMapping(typeName = "ExamBlueprint", field = "currentVersion")
    public CompletableFuture<ExamBlueprintVersionDto> currentVersion(ExamBlueprintDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamBlueprintVersionDto>> loader = env.getDataLoader("examBlueprintVersionsByBlueprintId");
        return loader.load(source.id()).thenApply(ExamBlueprintController::latestVersion);
    }

    @SchemaMapping(typeName = "ExamBlueprint", field = "sectionCount")
    public CompletableFuture<Integer> blueprintSectionCount(ExamBlueprintDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamBlueprintVersionDto>> versionsLoader = env.getDataLoader("examBlueprintVersionsByBlueprintId");
        DataLoader<UUID, List<ExamBlueprintSectionDto>> sectionsLoader = env.getDataLoader("examBlueprintSectionsByVersionId");
        return versionsLoader.load(source.id()).thenCompose(versions -> {
            var current = latestVersion(versions);
            if (current == null) {
                return CompletableFuture.completedFuture(0);
            }
            return sectionsLoader.load(current.id()).thenApply(List::size);
        });
    }

    @SchemaMapping(typeName = "ExamBlueprintVersion", field = "sections")
    public CompletableFuture<List<ExamBlueprintSectionDto>> sections(ExamBlueprintVersionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamBlueprintSectionDto>> loader = env.getDataLoader("examBlueprintSectionsByVersionId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "ExamBlueprintVersion", field = "sectionCount")
    public CompletableFuture<Integer> versionSectionCount(ExamBlueprintVersionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamBlueprintSectionDto>> loader = env.getDataLoader("examBlueprintSectionsByVersionId");
        return loader.load(source.id()).thenApply(List::size);
    }

    @SchemaMapping(typeName = "ExamBlueprintVersion", field = "slotCount")
    public CompletableFuture<Integer> slotCount(ExamBlueprintVersionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamBlueprintSlotDto>> loader = env.getDataLoader("examBlueprintSlotsByVersionId");
        return loader.load(source.id()).thenApply(List::size);
    }

    @SchemaMapping(typeName = "ExamBlueprintVersion", field = "weightSum")
    public CompletableFuture<BigDecimal> weightSum(ExamBlueprintVersionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamBlueprintSectionDto>> loader = env.getDataLoader("examBlueprintSectionsByVersionId");
        return loader.load(source.id()).thenApply(sections -> sections.stream()
            .map(section -> section.sectionWeight() == null ? BigDecimal.ZERO : section.sectionWeight())
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @SchemaMapping(typeName = "ExamBlueprintSection", field = "slots")
    public CompletableFuture<List<ExamBlueprintSlotDto>> slots(ExamBlueprintSectionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamBlueprintSlotDto>> loader = env.getDataLoader("examBlueprintSlotsBySectionId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "ExamBlueprintSlot", field = "fixedQuestion")
    public CompletableFuture<QuestionDto> fixedQuestion(ExamBlueprintSlotDto source, DataFetchingEnvironment env) {
        if (source.fixedQuestionId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        // Không cần questionByIdAccessible (OR-tree phân quyền) ở đây: muốn tới được field này
        // thì examBlueprint(id)/examBlueprintVersion(id) đã bắt buộc pass
        // ViewExamBlueprintDetailsUseCase.hasAccess trước rồi, nên lookup thẳng theo id là đủ.
        var startedAt = System.nanoTime();
        DataLoader<UUID, QuestionDto> loader = env.getDataLoader("questionByIdBasic");
        return loader.load(source.fixedQuestionId()).whenComplete((result, error) ->
            LOGGER.info("[blueprint-perf] fixedQuestion questionId={} tookMs={} error={}",
                source.fixedQuestionId(), (System.nanoTime() - startedAt) / 1_000_000, error != null ? error.toString() : "none")
        );
    }

    private static ExamBlueprintVersionDto latestVersion(List<ExamBlueprintVersionDto> versions) {
        return versions.stream().max(Comparator.comparingInt(ExamBlueprintVersionDto::version)).orElse(null);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
    }
}
