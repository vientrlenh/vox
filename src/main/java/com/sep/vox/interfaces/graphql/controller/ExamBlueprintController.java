package com.sep.vox.interfaces.graphql.controller;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
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
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;

@Controller("graphqlExamBlueprintController")
public class ExamBlueprintController {

    private final ViewExamBlueprintsUseCase viewExamBlueprintsUseCase;
    private final ViewExamBlueprintDetailsUseCase viewExamBlueprintDetailsUseCase;

    public ExamBlueprintController(
            ViewExamBlueprintsUseCase viewExamBlueprintsUseCase,
            ViewExamBlueprintDetailsUseCase viewExamBlueprintDetailsUseCase) {
        this.viewExamBlueprintsUseCase = viewExamBlueprintsUseCase;
        this.viewExamBlueprintDetailsUseCase = viewExamBlueprintDetailsUseCase;
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
        return viewExamBlueprintDetailsUseCase.execute(new ViewExamBlueprintDetailsQuery(id));
    }

    @SchemaMapping(typeName = "ExamBlueprint", field = "versions")
    public CompletableFuture<List<ExamBlueprintVersionDto>> versions(
            ExamBlueprintDto source,
            @Argument(name = "status") ExamBlueprintVersionStatus status,
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
        DataLoader<UUID, QuestionDto> loader = env.getDataLoader("questionByIdAccessible");
        return loader.load(source.fixedQuestionId());
    }

    private static ExamBlueprintVersionDto latestVersion(List<ExamBlueprintVersionDto> versions) {
        return versions.stream().max(Comparator.comparingInt(ExamBlueprintVersionDto::version)).orElse(null);
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || size == null || page < 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
    }
}
