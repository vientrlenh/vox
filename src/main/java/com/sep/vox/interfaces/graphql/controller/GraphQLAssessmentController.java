package com.sep.vox.interfaces.graphql.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;

import com.sep.vox.application.port.input.query.CanViewAssessmentBlueprintDataQuery;
import com.sep.vox.application.port.input.query.EstimateAssessmentTokenQuotaQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentDetailsQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentPaperDetailsQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentStatusCountsQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentsQuery;
import com.sep.vox.application.port.input.query.ViewMyAssessmentRoleQuery;
import com.sep.vox.application.port.input.usecase.assessment.CanViewAssessmentBlueprintDataUseCase;
import com.sep.vox.application.port.input.usecase.assessment.EstimateAssessmentTokenQuotaUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentDetailsUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentStatusCountsUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewMyAssessmentRoleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.ViewAssessmentPaperDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.dto.AssessmentStatusCountsDto;
import com.sep.vox.application.response.input.assessment.AssessmentTokenEstimateResponse;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.dto.assessment.AssessmentBlueprintDto;
import com.sep.vox.domain.dto.assessment.AssessmentBlueprintSlotDto;
import com.sep.vox.domain.dto.assessment.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.dto.assessment.AssessmentDto;
import com.sep.vox.domain.dto.assessment.AssessmentMemberDto;
import com.sep.vox.domain.dto.assessment.AssessmentPaperDto;
import com.sep.vox.domain.dto.assessment.AssessmentPaperItemDto;
import com.sep.vox.domain.dto.assessment.AssessmentPaperSectionDto;
import com.sep.vox.domain.dto.assessment.AssessmentScheduleDto;
import com.sep.vox.domain.dto.assessment.AssessmentSecurePoolDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Controller
@RequiredArgsConstructor 
public class GraphQLAssessmentController {

    private final ViewAssessmentsUseCase viewAssessmentsUseCase;
    private final ViewAssessmentDetailsUseCase viewAssessmentDetailsUseCase;
    private final ViewAssessmentPaperDetailsUseCase viewAssessmentPaperDetailsUseCase;
    private final ViewAssessmentStatusCountsUseCase viewAssessmentStatusCountsUseCase;
    private final ViewMyAssessmentRoleUseCase viewMyAssessmentRoleUseCase;
    private final CanViewAssessmentBlueprintDataUseCase canViewAssessmentBlueprintDataUseCase;
    private final EstimateAssessmentTokenQuotaUseCase estimateAssessmentTokenQuotaUseCase;
    private final UserContextPort userContextPort;

    @QueryMapping(name = "assessments")
    public PageResult<AssessmentDto> assessments(
            @Argument(name = "kind") AssessmentKind kind,
            @Argument(name = "status") AssessmentStatus status,
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePage(page, size);
        return viewAssessmentsUseCase.execute(new ViewAssessmentsQuery(kind, status, schoolId, null, keyword, page, size));
    }

    @QueryMapping(name = "classTests")
    public PageResult<AssessmentDto> classTests(
            @Argument(name = "status") AssessmentStatus status,
            @Argument(name = "schoolClassId") UUID schoolClassId,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePage(page, size);
        return viewAssessmentsUseCase.execute(
            new ViewAssessmentsQuery(AssessmentKind.CLASS_TEST, status, null, schoolClassId, keyword, page, size)
        );
    }

    @QueryMapping(name = "assessment")
    public AssessmentDto assessment(@Argument(name = "id") UUID id) {
        return viewAssessmentDetailsUseCase.execute(new ViewAssessmentDetailsQuery(id));
    }

    @QueryMapping(name = "assessmentMyRole")
    public String assessmentMyRole(@Argument(name = "assessmentId") UUID assessmentId) {
        return viewMyAssessmentRoleUseCase.execute(new ViewMyAssessmentRoleQuery(assessmentId));
    }

    @QueryMapping(name = "assessmentPaper")
    public AssessmentPaperDto assessmentPaper(@Argument(name = "id") UUID id) {
        return viewAssessmentPaperDetailsUseCase.execute(new ViewAssessmentPaperDetailsQuery(id));
    }

    @QueryMapping(name = "assessmentTokenEstimate")
    public AssessmentTokenEstimateResponse assessmentTokenEstimate(@Argument(name = "assessmentId") UUID assessmentId) {
        return estimateAssessmentTokenQuotaUseCase.execute(new EstimateAssessmentTokenQuotaQuery(assessmentId));
    }

    @QueryMapping(name = "assessmentStatusCounts")
    public AssessmentStatusCountsDto assessmentStatusCounts(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "kind") AssessmentKind kind,
            @Argument(name = "dateFrom") Instant dateFrom,
            @Argument(name = "dateTo") Instant dateTo) {
        return viewAssessmentStatusCountsUseCase.execute(new ViewAssessmentStatusCountsQuery(
            schoolId,
            kind,
            dateFrom, 
            dateTo
        ));
    }

    @SchemaMapping(typeName = "Assessment", field = "blueprint")
    public CompletableFuture<AssessmentBlueprintDto> blueprint(AssessmentDto source, DataFetchingEnvironment env) {
        if (source.blueprintId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return canViewAssessmentBlueprintData(source, env).thenCompose(canView -> {
            if (!canView) {
                return CompletableFuture.completedFuture(null);
            }
            DataLoader<UUID, AssessmentBlueprintDto> loader = env.getDataLoader("assessmentBlueprintById");
            return loader.load(source.blueprintId());
        });
    }

    @SchemaMapping(typeName = "Assessment", field = "blueprintVersion")
    public CompletableFuture<AssessmentBlueprintVersionDto> blueprintVersion(AssessmentDto source, DataFetchingEnvironment env) {
        if (source.blueprintVersionId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return canViewAssessmentBlueprintData(source, env).thenCompose(canView -> {
            if (!canView) {
                return CompletableFuture.completedFuture(null);
            }
            DataLoader<UUID, AssessmentBlueprintVersionDto> loader = env.getDataLoader("assessmentBlueprintVersionById");
            return loader.load(source.blueprintVersionId());
        });
    }

    @SchemaMapping(typeName = "Assessment", field = "candidateCount")
    public CompletableFuture<Integer> candidateCount(AssessmentDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, Integer> loader = env.getDataLoader("assessmentCandidateCountByAssessmentId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Assessment", field = "members")
    public CompletableFuture<List<AssessmentMemberDto>> members(AssessmentDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<AssessmentMemberDto>> loader = env.getDataLoader("assessmentMembersByAssessmentId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "AssessmentMember", field = "user")
    public CompletableFuture<UserDto> assessmentMemberUser(AssessmentMemberDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(source.userId());
    }

    @SchemaMapping(typeName = "Assessment", field = "papers")
    public CompletableFuture<List<AssessmentPaperDto>> papers(
            AssessmentDto source,
            @Argument(name = "status") AssessmentPaperStatus status,
            DataFetchingEnvironment env) {
        DataLoader<UUID, List<AssessmentPaperDto>> loader = env.getDataLoader("assessmentPapersByAssessmentId");
        return loader.load(source.id()).thenApply(papers -> status == null
            ? papers
            : papers.stream().filter(paper -> status.name().equals(paper.status())).toList());
    }

    @SchemaMapping(typeName = "AssessmentPaper", field = "sections")
    public CompletableFuture<List<AssessmentPaperSectionDto>> sections(AssessmentPaperDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<AssessmentPaperSectionDto>> loader = env.getDataLoader("assessmentPaperSectionsByPaperId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "AssessmentPaperSection", field = "items")
    public CompletableFuture<List<AssessmentPaperItemDto>> items(AssessmentPaperSectionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<AssessmentPaperItemDto>> loader = env.getDataLoader("assessmentPaperItemsBySectionId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Assessment", field = "schedules")
    public CompletableFuture<List<AssessmentScheduleDto>> schedules(AssessmentDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<AssessmentScheduleDto>> loader = env.getDataLoader("assessmentSchedulesByAssessmentId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Assessment", field = "securePool")
    public CompletableFuture<AssessmentSecurePoolDto> securePool(AssessmentDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, AssessmentSecurePoolDto> loader = env.getDataLoader("assessmentSecurePoolByAssessmentId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Assessment", field = "schoolClassId")
    public CompletableFuture<UUID> schoolClassId(AssessmentDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, UUID> loader = env.getDataLoader("assessmentSchoolClassIdByAssessmentId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "AssessmentPaperItem", field = "question")
    public CompletableFuture<QuestionDto> question(AssessmentPaperItemDto source, DataFetchingEnvironment env) {
        if (source.questionId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, QuestionDto> loader = env.getDataLoader("questionByIdAccessible");
        return loader.load(source.questionId());
    }

    @SchemaMapping(typeName = "AssessmentPaperItem", field = "slotType")
    public CompletableFuture<AssessmentBlueprintSlotType> assessmentPaperItemSlotType(AssessmentPaperItemDto source, DataFetchingEnvironment env) {
        return loadBlueprintSlot(source, env)
            .thenApply(slot -> slot == null ? null : slot.slotType());
    }

    @SchemaMapping(typeName = "AssessmentPaperItem", field = "selectionSpec")
    public CompletableFuture<QuestionSelectionSpec> assessmentPaperItemSelectionSpec(
            AssessmentPaperItemDto source, DataFetchingEnvironment env) {
        return loadBlueprintSlot(source, env)
            .thenApply(slot -> slot == null ? null : slot.selectionSpec());
    }

    /**
     * Không cần check quyền riêng: AssessmentPaperItem chỉ tới được qua AssessmentPaper.sections.items, mà
     * ViewAssessmentPaperDetailsUseCase đã uỷ quyền check cho ViewAssessmentDetailsUseCase. Cố tình KHÔNG dùng
     * canViewAssessmentBlueprintData ở đây - nó chỉ true với SCHOOL_ADMIN/kỳ thi đã đóng, sẽ giấu tiêu chí
     * khỏi chính AUTHOR đang cần chọn câu cho ô SELECTION.
     */
    private CompletableFuture<AssessmentBlueprintSlotDto> loadBlueprintSlot(
            AssessmentPaperItemDto source, DataFetchingEnvironment env) {
        if (source.blueprintSlotId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, AssessmentBlueprintSlotDto> loader = env.getDataLoader("assessmentBlueprintSlotById");
        return loader.load(source.blueprintSlotId());
    }

    private CompletableFuture<Boolean> canViewAssessmentBlueprintData(AssessmentDto source, DataFetchingEnvironment env) {
        if (canViewAssessmentBlueprintDataUseCase.execute(
                new CanViewAssessmentBlueprintDataQuery(source.schoolId(), source.status().name()))) {
            return CompletableFuture.completedFuture(true);
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        DataLoader<UUID, List<AssessmentMemberDto>> loader = env.getDataLoader("assessmentMembersByAssessmentId");
        return loader.load(source.id()).thenApply(members -> members.stream()
            .anyMatch(member -> member.userId().equals(currentUserId)));
    }

    private void validatePage(Integer page, Integer size) {
        if (page == null || size == null || page < 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
    }
}
