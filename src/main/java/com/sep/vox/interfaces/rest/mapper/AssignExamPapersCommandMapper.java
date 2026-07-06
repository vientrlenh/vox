package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AssignExamPapersCommand;
import com.sep.vox.application.port.input.command.ExamPaperAssignmentItem;
import com.sep.vox.interfaces.rest.dto.request.AssignExamPapersRequest;

public final class AssignExamPapersCommandMapper {

    private AssignExamPapersCommandMapper() {
    }

    public static AssignExamPapersCommand fromRequest(UUID examId, AssignExamPapersRequest request) {
        List<ExamPaperAssignmentItem> assignments = request.assignments() == null ? List.of()
            : request.assignments().stream()
                .map(item -> new ExamPaperAssignmentItem(item.candidateId(), item.paperId()))
                .toList();
        return new AssignExamPapersCommand(examId, assignments);
    }
}
