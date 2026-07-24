package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

import com.sep.vox.application.port.input.command.UpdateExamCandidateStatusCommand;
import com.sep.vox.application.port.input.command.UpdateExamCandidatesAttendanceCommand;
import com.sep.vox.application.port.input.query.ViewExamCandidatesQuery;
import com.sep.vox.application.port.input.usecase.examcandidate.UpdateExamCandidatesAttendanceUseCase;
import com.sep.vox.application.port.input.usecase.examcandidate.UpdateExamCandidateStatusUseCase;
import com.sep.vox.application.port.input.usecase.examcandidate.ViewExamCandidatesUseCase;
import com.sep.vox.application.query.dto.ExamAttemptSummary;
import com.sep.vox.application.query.dto.ExamCandidateAttempts;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;

@Controller("graphqlExamCandidateController")
public class ExamCandidateController {

    private final ViewExamCandidatesUseCase viewExamCandidatesUseCase;
    private final UpdateExamCandidateStatusUseCase updateExamCandidateStatusUseCase;
    private final UpdateExamCandidatesAttendanceUseCase updateExamCandidatesAttendanceUseCase;

    public ExamCandidateController(
            ViewExamCandidatesUseCase viewExamCandidatesUseCase,
            UpdateExamCandidateStatusUseCase updateExamCandidateStatusUseCase,
            UpdateExamCandidatesAttendanceUseCase updateExamCandidatesAttendanceUseCase) {
        this.viewExamCandidatesUseCase = viewExamCandidatesUseCase;
        this.updateExamCandidateStatusUseCase = updateExamCandidateStatusUseCase;
        this.updateExamCandidatesAttendanceUseCase = updateExamCandidatesAttendanceUseCase;
    }

    @QueryMapping(name = "examCandidates")
    public List<ExamCandidateDto> examCandidates(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "scheduleId") UUID scheduleId,
            @Argument(name = "status") ExamCandidateStatus status) {
        return viewExamCandidatesUseCase.execute(new ViewExamCandidatesQuery(examId, scheduleId, status));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public UUID updateExamCandidateStatus(
            @Argument(name = "candidateId") UUID candidateId,
            @Argument(name = "status") ExamCandidateStatus status) {
        return updateExamCandidateStatusUseCase.execute(new UpdateExamCandidateStatusCommand(candidateId, status)).id();
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public List<ExamCandidateDto> updateExamCandidatesAttendance(
            @Argument(name = "scheduleId") UUID scheduleId,
            @Argument(name = "candidateIds") List<UUID> candidateIds) {
        return updateExamCandidatesAttendanceUseCase.execute(new UpdateExamCandidatesAttendanceCommand(scheduleId, candidateIds));
    }

    @SchemaMapping(typeName = "ExamCandidate", field = "student")
    public CompletableFuture<UserDto> student(ExamCandidateDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(source.studentId());
    }

    @SchemaMapping(typeName = "ExamCandidate", field = "assignedPaper")
    public CompletableFuture<ExamPaperDto> assignedPaper(ExamCandidateDto source, DataFetchingEnvironment env) {
        if (source.assignedPaperId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, ExamPaperDto> loader = env.getDataLoader("examPaperById");
        return loader.load(source.assignedPaperId());
    }

    @SchemaMapping(typeName = "ExamCandidate", field = "schedule")
    public CompletableFuture<ExamScheduleDto> schedule(ExamCandidateDto source, DataFetchingEnvironment env) {
        if (source.scheduleId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, ExamScheduleDto> loader = env.getDataLoader("examScheduleById");
        return loader.load(source.scheduleId());
    }

    @SchemaMapping(typeName = "ExamCandidate", field = "exam")
    public CompletableFuture<ExamDto> exam(ExamCandidateDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, ExamDto> loader = env.getDataLoader("examById");
        return loader.load(source.examId());
    }

    @SchemaMapping(typeName = "ExamCandidate", field = "latestSessionId")
    public CompletableFuture<UUID> latestSessionId(ExamCandidateDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, ExamCandidateAttempts> loader =
            env.getDataLoader("examCandidateAttemptsByCandidateId");
        return loader.load(source.id())
            .thenApply(result -> result == null || result.attempts().isEmpty() ? null : result.attempts().get(0).sessionId());
    }

    @SchemaMapping(typeName = "ExamCandidate", field = "attempts")
    public CompletableFuture<List<ExamAttemptSummary>> attempts(
            ExamCandidateDto source,
            DataFetchingEnvironment env) {
        DataLoader<UUID, ExamCandidateAttempts> loader =
            env.getDataLoader("examCandidateAttemptsByCandidateId");
        return loader.load(source.id())
            .thenApply(result -> result == null ? List.of() : result.attempts());
    }

    @SchemaMapping(typeName = "ExamCandidate", field = "officialAttempt")
    public CompletableFuture<ExamAttemptSummary> officialAttempt(
            ExamCandidateDto source,
            DataFetchingEnvironment env) {
        DataLoader<UUID, ExamCandidateAttempts> loader =
            env.getDataLoader("examCandidateAttemptsByCandidateId");
        return loader.load(source.id())
            .thenApply(result -> result == null ? null : result.officialAttempt());
    }

    @SchemaMapping(typeName = "ExamCandidate", field = "officialScore")
    public CompletableFuture<java.math.BigDecimal> officialScore(
            ExamCandidateDto source,
            DataFetchingEnvironment env) {
        DataLoader<UUID, ExamCandidateAttempts> loader =
            env.getDataLoader("examCandidateAttemptsByCandidateId");
        return loader.load(source.id())
            .thenApply(result -> result == null ? null : result.officialScore());
    }
}
