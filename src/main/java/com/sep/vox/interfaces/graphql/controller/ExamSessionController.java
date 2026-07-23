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

import com.sep.vox.application.port.input.query.GetExamRecordsQuery;
import com.sep.vox.application.port.input.usecase.recording.GetExamRecordsUseCase;
import com.sep.vox.application.port.input.usecase.recording.ViewMyRecordingsUseCase;
import com.sep.vox.application.query.dto.ExamItemResponseDto;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.dto.ExamRecordingDto;

@Controller("graphqlExamSessionController")
public class ExamSessionController {
    
    private final GetExamRecordsUseCase getExamRecordsUseCase;
    private final ViewMyRecordingsUseCase viewMyRecordingsUseCase;

    public ExamSessionController(GetExamRecordsUseCase getExamRecordsUseCase, ViewMyRecordingsUseCase viewMyRecordingsUseCase) {
        this.getExamRecordsUseCase = getExamRecordsUseCase;
        this.viewMyRecordingsUseCase = viewMyRecordingsUseCase;
    }

    @QueryMapping("records")
    public List<ExamRecordingDto> records(@Argument(name = "examSessionId") UUID examSessionId, @Argument(name = "streamType") String streamType) {
        var query = new GetExamRecordsQuery(examSessionId, streamType);
        var data = getExamRecordsUseCase.execute(query);
        return data;
    }

    @QueryMapping("myRecordings")
    public List<ExamItemResponseDto> myRecordings() {
        return viewMyRecordingsUseCase.execute(null);
    }

    @SchemaMapping(typeName = "ExamItemResponse", field = "exam")
    public CompletableFuture<ExamDto> exam(ExamItemResponseDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, ExamDto> loader = env.getDataLoader("examById");
        return loader.load(source.examId());
    }
}
