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
}
