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

import com.sep.vox.application.port.input.query.ViewExamCandidatesQuery;
import com.sep.vox.application.port.input.usecase.examcandidate.ViewExamCandidatesUseCase;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;

@Controller("graphqlExamCandidateController")
public class ExamCandidateController {

    private final ViewExamCandidatesUseCase viewExamCandidatesUseCase;

    public ExamCandidateController(ViewExamCandidatesUseCase viewExamCandidatesUseCase) {
        this.viewExamCandidatesUseCase = viewExamCandidatesUseCase;
    }

    @QueryMapping(name = "examCandidates")
    public List<ExamCandidateDto> examCandidates(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "scheduleId") UUID scheduleId,
            @Argument(name = "status") ExamCandidateStatus status) {
        return viewExamCandidatesUseCase.execute(new ViewExamCandidatesQuery(examId, scheduleId, status));
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
}
