package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AssignGradingCommand;
import com.sep.vox.application.port.input.command.AutoAssignGradingCommand;
import com.sep.vox.application.port.input.command.GradingDecisionCommand;
import com.sep.vox.application.port.input.command.ReassignGradingCommand;
import com.sep.vox.application.port.input.command.ReclaimOverdueAssignmentsCommand;
import com.sep.vox.application.port.input.command.RemoveGradingAssignmentCommand;
import com.sep.vox.application.port.input.command.SetGradingDeadlineCommand;
import com.sep.vox.application.port.input.command.SubmitGradingCommand;
import com.sep.vox.interfaces.rest.dto.request.AssignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.AutoAssignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.GradingDecisionRequest;
import com.sep.vox.interfaces.rest.dto.request.ReassignGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.ReclaimOverdueAssignmentsRequest;
import com.sep.vox.interfaces.rest.dto.request.SetGradingDeadlineRequest;
import com.sep.vox.interfaces.rest.dto.request.SubmitGradingRequest;

public final class ExamGradingCommandMapper {

    private ExamGradingCommandMapper() {}

    public static AssignGradingCommand fromRequest(AssignGradingRequest request) {
        return new AssignGradingCommand(
            request.roundType(),
            request.deadlineAt(),
            request.assignments() == null ? List.of() : request.assignments().stream()
                .map(item -> new AssignGradingCommand.AssignmentItem(item.candidateResultId(), item.teacherId()))
                .toList()
        );
    }

    public static AutoAssignGradingCommand fromRequest(AutoAssignGradingRequest request) {
        return new AutoAssignGradingCommand(
            request.examId(),
            request.scheduleId(),
            request.roundType(),
            request.selectionMode(),
            request.percent(),
            request.candidateResultIds(),
            request.deadlineAt(),
            request.teacherIds()
        );
    }

    public static ReassignGradingCommand fromRequest(UUID assignmentId, ReassignGradingRequest request) {
        return new ReassignGradingCommand(assignmentId, request.teacherId());
    }

    public static RemoveGradingAssignmentCommand toRemoveCommand(UUID assignmentId) {
        return new RemoveGradingAssignmentCommand(assignmentId);
    }

    /** Dùng cho cả /grade lẫn /grade/preview — cùng body, cùng command. */
    public static SubmitGradingCommand fromRequest(UUID assignmentId, SubmitGradingRequest request) {
        return new SubmitGradingCommand(
            assignmentId,
            request.items() == null ? List.of() : request.items().stream()
                .map(item -> new SubmitGradingCommand.ItemGrade(
                    item.paperItemId(),
                    item.criterionScores() == null ? List.of() : item.criterionScores().stream()
                        .map(score -> new SubmitGradingCommand.CriterionScoreItem(
                            score.rubricCriterionId(), score.score(), score.rationale()))
                        .toList(),
                    item.feedbackSummary()
                ))
                .toList()
        );
    }

    public static SetGradingDeadlineCommand fromRequest(SetGradingDeadlineRequest request) {
        return new SetGradingDeadlineCommand(request.assignmentIds(), request.deadlineAt());
    }

    public static ReclaimOverdueAssignmentsCommand fromRequest(ReclaimOverdueAssignmentsRequest request) {
        return new ReclaimOverdueAssignmentsCommand(
            request.examId(),
            request.assignmentIds(),
            request.reassignToTeacherIds(),
            request.newDeadlineAt()
        );
    }

    /** Dùng chung cho /uphold, /invalidate, /clear-invalid và /decline — cùng một body. */
    public static GradingDecisionCommand toDecisionCommand(UUID assignmentId, GradingDecisionRequest request) {
        return new GradingDecisionCommand(assignmentId, request == null ? null : request.reason());
    }
}
