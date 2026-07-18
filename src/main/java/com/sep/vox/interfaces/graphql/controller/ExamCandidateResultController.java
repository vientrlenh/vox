package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

import com.sep.vox.application.port.input.query.ViewMyExamResultsQuery;
import com.sep.vox.application.port.input.usecase.examcandidateresult.ViewMyExamResultsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.ExamCandidateResultDto;
import com.sep.vox.domain.dto.ExamDto;

@Controller("graphqlExamCandidateResultController")
public class ExamCandidateResultController {

    private final ViewMyExamResultsUseCase viewMyExamResultsUseCase;

    public ExamCandidateResultController(ViewMyExamResultsUseCase viewMyExamResultsUseCase) {
        this.viewMyExamResultsUseCase = viewMyExamResultsUseCase;
    }

    @QueryMapping(name = "myExamResults")
    public PageResult<ExamCandidateResultDto> myExamResults(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        validatePage(page, size);
        return viewMyExamResultsUseCase.execute(new ViewMyExamResultsQuery(page, size));
    }

    @SchemaMapping(typeName = "ExamCandidateResult", field = "exam")
    public CompletableFuture<ExamDto> exam(ExamCandidateResultDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, ExamDto> loader = env.getDataLoader("examById");
        return loader.load(source.examId());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
    }
}
