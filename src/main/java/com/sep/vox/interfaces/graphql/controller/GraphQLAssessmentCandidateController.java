package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.interfaces.graphql.dataloader.GraphQLDataLoaders;
import com.sep.vox.interfaces.graphql.dataloader.UserGraphQLDataLoaderConfig;
import com.sep.vox.interfaces.graphql.dataloader.AssessmentGraphQLDataLoaderConfig;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

import com.sep.vox.application.port.input.command.assessmentcandidate.UpdateAssessmentCandidateStatusCommand;
import com.sep.vox.application.port.input.command.assessmentcandidate.UpdateAssessmentCandidatesAttendanceCommand;
import com.sep.vox.application.port.input.query.assessmentcandidate.ViewAssessmentCandidatesQuery;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UpdateAssessmentCandidatesAttendanceUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UpdateAssessmentCandidateStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ViewAssessmentCandidatesUseCase;
import com.sep.vox.application.query.dto.AssessmentAttemptSummary;
import com.sep.vox.application.query.dto.AssessmentCandidateAttempts;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.dto.AssessmentPaperDto;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;

@Controller
@RequiredArgsConstructor
public class GraphQLAssessmentCandidateController {

    private final ViewAssessmentCandidatesUseCase viewAssessmentCandidatesUseCase;
    private final UpdateAssessmentCandidateStatusUseCase updateAssessmentCandidateStatusUseCase;
    private final UpdateAssessmentCandidatesAttendanceUseCase updateAssessmentCandidatesAttendanceUseCase;

    @QueryMapping(name = "assessmentCandidates")
    public List<AssessmentCandidateDto> assessmentCandidates(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "scheduleId") UUID scheduleId,
            @Argument(name = "status") AssessmentCandidateStatus status) {
        return viewAssessmentCandidatesUseCase.execute(new ViewAssessmentCandidatesQuery(assessmentId, scheduleId, status));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public UUID updateAssessmentCandidateStatus(
            @Argument(name = "candidateId") UUID candidateId,
            @Argument(name = "status") AssessmentCandidateStatus status) {
        return updateAssessmentCandidateStatusUseCase.execute(new UpdateAssessmentCandidateStatusCommand(candidateId, status)).id();
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public List<AssessmentCandidateDto> updateAssessmentCandidatesAttendance(
            @Argument(name = "scheduleId") UUID scheduleId,
            @Argument(name = "candidateIds") List<UUID> candidateIds) {
        return updateAssessmentCandidatesAttendanceUseCase.execute(new UpdateAssessmentCandidatesAttendanceCommand(scheduleId, candidateIds));
    }

    @SchemaMapping(typeName = "AssessmentCandidate", field = "student")
    public CompletableFuture<UserDto> student(AssessmentCandidateDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = GraphQLDataLoaders.require(env, UserGraphQLDataLoaderConfig.USER_BY_USER_ID);
        return loader.load(source.studentId());
    }

    @SchemaMapping(typeName = "AssessmentCandidate", field = "assignedPaper")
    public CompletableFuture<AssessmentPaperDto> assignedPaper(AssessmentCandidateDto source, DataFetchingEnvironment env) {
        if (source.assignedPaperId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, AssessmentPaperDto> loader = GraphQLDataLoaders.require(env, AssessmentGraphQLDataLoaderConfig.ASSESSMENT_PAPER_BY_ASSESSMENT_PAPER_ID);
        return loader.load(source.assignedPaperId());
    }

    @SchemaMapping(typeName = "AssessmentCandidate", field = "schedule")
    public CompletableFuture<AssessmentScheduleDto> schedule(AssessmentCandidateDto source, DataFetchingEnvironment env) {
        if (source.scheduleId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, AssessmentScheduleDto> loader = GraphQLDataLoaders.require(env, AssessmentGraphQLDataLoaderConfig.ASSESSMENT_SCHEDULE_BY_ASSESSMENT_SCHEDULE_ID);
        return loader.load(source.scheduleId());
    }

    @SchemaMapping(typeName = "AssessmentCandidate", field = "assessment")
    public CompletableFuture<AssessmentDto> assessment(AssessmentCandidateDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, AssessmentDto> loader = GraphQLDataLoaders.require(env, AssessmentGraphQLDataLoaderConfig.ASSESSMENT_BY_ASSESSMENT_ID);
        return loader.load(source.assessmentId());
    }

    @SchemaMapping(typeName = "AssessmentCandidate", field = "latestSessionId")
    public CompletableFuture<UUID> latestSessionId(AssessmentCandidateDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, AssessmentCandidateAttempts> loader =
            GraphQLDataLoaders.require(env, AssessmentGraphQLDataLoaderConfig.ASSESSMENT_CANDIDATE_ATTEMPTS_BY_ASSESSMENT_CANDIDATE_ID);
        return loader.load(source.id())
            .thenApply(result -> result == null || result.attempts().isEmpty() ? null : result.attempts().get(0).sessionId());
    }

    @SchemaMapping(typeName = "AssessmentCandidate", field = "attempts")
    public CompletableFuture<List<AssessmentAttemptSummary>> attempts(
            AssessmentCandidateDto source,
            DataFetchingEnvironment env) {
        DataLoader<UUID, AssessmentCandidateAttempts> loader =
            GraphQLDataLoaders.require(env, AssessmentGraphQLDataLoaderConfig.ASSESSMENT_CANDIDATE_ATTEMPTS_BY_ASSESSMENT_CANDIDATE_ID);
        return loader.load(source.id())
            .thenApply(result -> result == null ? List.of() : result.attempts());
    }

    @SchemaMapping(typeName = "AssessmentCandidate", field = "officialAttempt")
    public CompletableFuture<AssessmentAttemptSummary> officialAttempt(
            AssessmentCandidateDto source,
            DataFetchingEnvironment env) {
        DataLoader<UUID, AssessmentCandidateAttempts> loader =
            GraphQLDataLoaders.require(env, AssessmentGraphQLDataLoaderConfig.ASSESSMENT_CANDIDATE_ATTEMPTS_BY_ASSESSMENT_CANDIDATE_ID);
        return loader.load(source.id())
            .thenApply(result -> result == null ? null : result.officialAttempt());
    }

    @SchemaMapping(typeName = "AssessmentCandidate", field = "officialScore")
    public CompletableFuture<java.math.BigDecimal> officialScore(
            AssessmentCandidateDto source,
            DataFetchingEnvironment env) {
        DataLoader<UUID, AssessmentCandidateAttempts> loader =
            GraphQLDataLoaders.require(env, AssessmentGraphQLDataLoaderConfig.ASSESSMENT_CANDIDATE_ATTEMPTS_BY_ASSESSMENT_CANDIDATE_ID);
        return loader.load(source.id())
            .thenApply(result -> result == null ? null : result.officialScore());
    }
}
