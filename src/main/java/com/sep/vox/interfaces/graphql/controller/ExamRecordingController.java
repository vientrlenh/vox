package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

import com.sep.vox.application.port.input.query.ViewMyRecordingsQuery;
import com.sep.vox.application.port.input.usecase.examrecording.ViewMyRecordingsUseCase;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.dto.ExamRecordingDto;

@Controller("graphqlExamRecordingController")
public class ExamRecordingController {

    private final ViewMyRecordingsUseCase viewMyRecordingsUseCase;

    public ExamRecordingController(ViewMyRecordingsUseCase viewMyRecordingsUseCase) {
        this.viewMyRecordingsUseCase = viewMyRecordingsUseCase;
    }

    @QueryMapping(name = "myRecordings")
    public List<ExamRecordingDto> myRecordings() {
        return viewMyRecordingsUseCase.execute(new ViewMyRecordingsQuery());
    }

    @SchemaMapping(typeName = "ExamRecording", field = "exam")
    public CompletableFuture<ExamDto> exam(ExamRecordingDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, ExamDto> loader = env.getDataLoader("examById");
        return loader.load(source.examId());
    }
}
