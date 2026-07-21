package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

import com.sep.vox.application.port.input.query.CanViewExamBlueprintDataQuery;
import com.sep.vox.application.port.input.query.ViewExamDetailsQuery;
import com.sep.vox.application.port.input.query.ViewExamPaperDetailsQuery;
import com.sep.vox.application.port.input.query.ViewExamStatusCountsQuery;
import com.sep.vox.application.port.input.query.ViewExamsQuery;
import com.sep.vox.application.port.input.query.ViewMyExamRoleQuery;
import com.sep.vox.application.port.input.usecase.exam.CanViewExamBlueprintDataUseCase;
import com.sep.vox.application.port.input.usecase.exam.ViewExamDetailsUseCase;
import com.sep.vox.application.port.input.usecase.exam.ViewExamStatusCountsUseCase;
import com.sep.vox.application.port.input.usecase.exam.ViewExamsUseCase;
import com.sep.vox.application.port.input.usecase.exam.ViewMyExamRoleUseCase;
import com.sep.vox.application.port.input.usecase.exampaper.ViewExamPaperDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ExamStatusCountsDto;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.dto.ExamMemberDto;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.dto.ExamPaperItemDto;
import com.sep.vox.domain.dto.ExamPaperSectionDto;
import com.sep.vox.domain.dto.ExamSecurePoolDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamStatus;

@Controller("graphqlExamController")
public class ExamController {

    private final ViewExamsUseCase viewExamsUseCase;
    private final ViewExamDetailsUseCase viewExamDetailsUseCase;
    private final ViewExamPaperDetailsUseCase viewExamPaperDetailsUseCase;
    private final ViewExamStatusCountsUseCase viewExamStatusCountsUseCase;
    private final ViewMyExamRoleUseCase viewMyExamRoleUseCase;
    private final CanViewExamBlueprintDataUseCase canViewExamBlueprintDataUseCase;
    private final UserContextPort userContextPort;

    public ExamController(
            ViewExamsUseCase viewExamsUseCase,
            ViewExamDetailsUseCase viewExamDetailsUseCase,
            ViewExamPaperDetailsUseCase viewExamPaperDetailsUseCase,
            ViewExamStatusCountsUseCase viewExamStatusCountsUseCase,
            ViewMyExamRoleUseCase viewMyExamRoleUseCase,
            CanViewExamBlueprintDataUseCase canViewExamBlueprintDataUseCase,
            UserContextPort userContextPort) {
        this.viewExamsUseCase = viewExamsUseCase;
        this.viewExamDetailsUseCase = viewExamDetailsUseCase;
        this.viewExamPaperDetailsUseCase = viewExamPaperDetailsUseCase;
        this.viewExamStatusCountsUseCase = viewExamStatusCountsUseCase;
        this.viewMyExamRoleUseCase = viewMyExamRoleUseCase;
        this.canViewExamBlueprintDataUseCase = canViewExamBlueprintDataUseCase;
        this.userContextPort = userContextPort;
    }

    @QueryMapping(name = "exams")
    public PageResult<ExamDto> exams(
            @Argument ExamKind kind,
            @Argument ExamStatus status,
            @Argument UUID schoolId,
            @Argument String keyword,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        validatePage(page, size);
        return viewExamsUseCase.execute(new ViewExamsQuery(kind, status, schoolId, null, keyword, page, size));
    }

    @QueryMapping(name = "classTests")
    public PageResult<ExamDto> classTests(
            @Argument ExamStatus status,
            @Argument UUID schoolClassId,
            @Argument String keyword,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        validatePage(page, size);
        return viewExamsUseCase.execute(
            new ViewExamsQuery(ExamKind.CLASS_TEST, status, null, schoolClassId, keyword, page, size)
        );
    }

    @QueryMapping(name = "exam")
    public ExamDto exam(@Argument(name = "id") UUID id) {
        return viewExamDetailsUseCase.execute(new ViewExamDetailsQuery(id));
    }

    @QueryMapping(name = "examMyRole")
    public String examMyRole(@Argument(name = "examId") UUID examId) {
        return viewMyExamRoleUseCase.execute(new ViewMyExamRoleQuery(examId));
    }

    @QueryMapping(name = "examPaper")
    public ExamPaperDto examPaper(@Argument(name = "id") UUID id) {
        return viewExamPaperDetailsUseCase.execute(new ViewExamPaperDetailsQuery(id));
    }

    @QueryMapping(name = "examStatusCounts")
    public ExamStatusCountsDto examStatusCounts(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "kind") ExamKind kind) {
        return viewExamStatusCountsUseCase.execute(new ViewExamStatusCountsQuery(schoolId, kind));
    }

    @SchemaMapping(typeName = "Exam", field = "blueprint")
    public CompletableFuture<ExamBlueprintDto> blueprint(ExamDto source, DataFetchingEnvironment env) {
        if (source.blueprintId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return canViewExamBlueprintData(source, env).thenCompose(canView -> {
            if (!canView) {
                return CompletableFuture.completedFuture(null);
            }
            DataLoader<UUID, ExamBlueprintDto> loader = env.getDataLoader("examBlueprintById");
            return loader.load(source.blueprintId());
        });
    }

    @SchemaMapping(typeName = "Exam", field = "blueprintVersion")
    public CompletableFuture<ExamBlueprintVersionDto> blueprintVersion(ExamDto source, DataFetchingEnvironment env) {
        if (source.blueprintVersionId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return canViewExamBlueprintData(source, env).thenCompose(canView -> {
            if (!canView) {
                return CompletableFuture.completedFuture(null);
            }
            DataLoader<UUID, ExamBlueprintVersionDto> loader = env.getDataLoader("examBlueprintVersionById");
            return loader.load(source.blueprintVersionId());
        });
    }

    @SchemaMapping(typeName = "Exam", field = "candidateCount")
    public CompletableFuture<Integer> candidateCount(ExamDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, Integer> loader = env.getDataLoader("examCandidateCountByExamId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Exam", field = "members")
    public CompletableFuture<List<ExamMemberDto>> members(ExamDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamMemberDto>> loader = env.getDataLoader("examMembersByExamId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "ExamMember", field = "user")
    public CompletableFuture<UserDto> examMemberUser(ExamMemberDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(source.userId());
    }

    @SchemaMapping(typeName = "Exam", field = "papers")
    public CompletableFuture<List<ExamPaperDto>> papers(
            ExamDto source,
            @Argument ExamPaperStatus status,
            DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamPaperDto>> loader = env.getDataLoader("examPapersByExamId");
        return loader.load(source.id()).thenApply(papers -> status == null
            ? papers
            : papers.stream().filter(paper -> status.name().equals(paper.status())).toList());
    }

    @SchemaMapping(typeName = "ExamPaper", field = "sections")
    public CompletableFuture<List<ExamPaperSectionDto>> sections(ExamPaperDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamPaperSectionDto>> loader = env.getDataLoader("examPaperSectionsByPaperId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "ExamPaperSection", field = "items")
    public CompletableFuture<List<ExamPaperItemDto>> items(ExamPaperSectionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<ExamPaperItemDto>> loader = env.getDataLoader("examPaperItemsBySectionId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Exam", field = "securePool")
    public CompletableFuture<ExamSecurePoolDto> securePool(ExamDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, ExamSecurePoolDto> loader = env.getDataLoader("examSecurePoolByExamId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Exam", field = "schoolClassId")
    public CompletableFuture<UUID> schoolClassId(ExamDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, UUID> loader = env.getDataLoader("examSchoolClassIdByExamId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "ExamPaperItem", field = "question")
    public CompletableFuture<QuestionDto> question(ExamPaperItemDto source, DataFetchingEnvironment env) {
        if (source.questionId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, QuestionDto> loader = env.getDataLoader("questionByIdAccessible");
        return loader.load(source.questionId());
    }

    private CompletableFuture<Boolean> canViewExamBlueprintData(ExamDto source, DataFetchingEnvironment env) {
        if (canViewExamBlueprintDataUseCase.execute(
                new CanViewExamBlueprintDataQuery(source.schoolId(), source.status()))) {
            return CompletableFuture.completedFuture(true);
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        DataLoader<UUID, List<ExamMemberDto>> loader = env.getDataLoader("examMembersByExamId");
        return loader.load(source.id()).thenApply(members -> members.stream()
            .anyMatch(member -> member.userId().equals(currentUserId)));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
    }
}
