package com.sep.vox.interfaces.rest.mapper;

import java.util.List;

import com.sep.vox.application.port.input.command.ClassTestQuestionCommand;
import com.sep.vox.interfaces.rest.dto.request.ClassTestQuestionRequest;

final class ClassTestQuestionCommandMapper {

    private ClassTestQuestionCommandMapper() {
    }

    static List<ClassTestQuestionCommand> fromRequests(List<ClassTestQuestionRequest> requests) {
        if (requests == null) {
            return null;
        }
        return requests.stream()
            .map(request -> new ClassTestQuestionCommand(request.questionId(), request.weight()))
            .toList();
    }
}
