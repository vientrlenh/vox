package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.GetExamRecordsQuery;
import com.sep.vox.application.port.input.usecase.recording.GetExamRecordsUseCase;
import com.sep.vox.domain.dto.ExamRecordingDto;

@Controller("graphqlExamSessionController")
public class ExamSessionController {
    
    private final GetExamRecordsUseCase getExamRecordsUseCase;

    public ExamSessionController(GetExamRecordsUseCase getExamRecordsUseCase) {
        this.getExamRecordsUseCase = getExamRecordsUseCase;
    }

    @QueryMapping("records")
    public List<ExamRecordingDto> records(@Argument(name = "examSessionId") UUID examSessionId, @Argument(name = "streamType") String streamType) {
        var query = new GetExamRecordsQuery(examSessionId, streamType);
        var data = getExamRecordsUseCase.execute(query);
        return data;
    }
}
