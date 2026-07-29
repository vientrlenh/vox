package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AssignGradingCommand;
import com.sep.vox.application.port.input.command.AutoAssignGradingCommand;
import com.sep.vox.application.port.input.command.InvalidateGradingCommand;
import com.sep.vox.application.port.input.command.ReassignGradingCommand;
import com.sep.vox.application.port.input.command.RemoveGradingAssignmentCommand;
import com.sep.vox.application.port.input.command.SubmitGradingCommand;
import com.sep.vox.interfaces.rest.dto.request.AssignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.AutoAssignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.InvalidateGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.ReassignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.SubmitGradingRequest;

public final class ExamGradingCommandMapper {

    private ExamGradingCommandMapper() {}

    public static AssignGradingCommand fromRequest(AssignGradingRequest request) {
        return new AssignGradingCommand(
            request.assignments() == null ? List.of() : request.assignments().stream()
                .map(item -> new AssignGradingCommand.AssignmentItem(item.candidateResultId(), item.teacherId()))
                .toList()
        );
    }

    public static AutoAssignGradingCommand fromRequest(AutoAssignGradingRequest request) {
        return new AutoAssignGradingCommand(request.examId(), request.scheduleId(), request.teacherIds());
    }

    public static ReassignGradingCommand fromRequest(UUID assignmentId, ReassignGradingRequest request) {
        return new ReassignGradingCommand(assignmentId, request.teacherId());
    }

    public static RemoveGradingAssignmentCommand toRemoveCommand(UUID assignmentId) {
        return new RemoveGradingAssignmentCommand(assignmentId);
    }

    /** Dùng cho cả /grade lẫn /grade/preview — cùng body, cùng command. */
    public static SubmitGradingCommand fromRequest(UUID assignmentId, SubmitGradingRequest request) {
        return new SubmitGradingCommand(assignmentId, null, toItemGrades(request));
    }

    /** Luồng nhà trường chấm trực tiếp theo candidateResultId, không qua phân công. */
    public static SubmitGradingCommand fromResultRequest(UUID candidateResultId, SubmitGradingRequest request) {
        return new SubmitGradingCommand(null, candidateResultId, toItemGrades(request));
    }

    private static List<SubmitGradingCommand.ItemGrade> toItemGrades(SubmitGradingRequest request) {
        return request.items() == null ? List.of() : request.items().stream()
            .map(item -> new SubmitGradingCommand.ItemGrade(
                item.paperItemId(),
                item.criterionScores() == null ? List.of() : item.criterionScores().stream()
                    .map(score -> new SubmitGradingCommand.CriterionScoreItem(
                        score.rubricCriterionId(), score.score(), score.rationale()))
                    .toList(),
                item.feedbackSummary()
            ))
            .toList();
    }

    public static InvalidateGradingCommand fromRequest(UUID assignmentId, InvalidateGradingRequest request) {
        return new InvalidateGradingCommand(assignmentId, null, request == null ? null : request.reason());
    }

    /** Luồng nhà trường chấm trực tiếp theo candidateResultId, không qua phân công. */
    public static InvalidateGradingCommand fromResultRequest(UUID candidateResultId, InvalidateGradingRequest request) {
        return new InvalidateGradingCommand(null, candidateResultId, request == null ? null : request.reason());
    }
}
