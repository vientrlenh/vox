package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.ApproveExamAppealCommand;
import com.sep.vox.application.port.input.command.AssignExamAppealReviewersCommand;
import com.sep.vox.application.port.input.command.CreateExamAppealCommand;
import com.sep.vox.application.port.input.command.PublishExamAppealCommand;
import com.sep.vox.application.port.input.command.RejectExamAppealCommand;
import com.sep.vox.application.port.input.command.RemoveExamAppealReviewerCommand;
import com.sep.vox.application.port.input.command.SubmitExamAppealReportCommand;
import com.sep.vox.interfaces.rest.dto.request.ApproveExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.AssignExamAppealReviewersRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.PublishExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.RejectExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.SubmitExamAppealReportRequest;

public final class ExamAppealCommandMapper {

    private ExamAppealCommandMapper() {}

    public static CreateExamAppealCommand fromRequest(CreateExamAppealRequest request) {
        return new CreateExamAppealCommand(
            request.candidateResultId(),
            request.paperItemIds(),
            request.reason(),
            request.notes()
        );
    }

    public static ApproveExamAppealCommand fromRequest(UUID appealId, ApproveExamAppealRequest request) {
        return new ApproveExamAppealCommand(
            appealId, 
            DateMapper.toInstant(request.deadline())
        );
    }

    public static RejectExamAppealCommand fromRequest(UUID appealId, RejectExamAppealRequest request) {
        return new RejectExamAppealCommand(appealId, request.reason());
    }

    public static AssignExamAppealReviewersCommand fromRequest(
            UUID appealId, AssignExamAppealReviewersRequest request) {
        return new AssignExamAppealReviewersCommand(appealId, request.reviewerIds());
    }

    public static RemoveExamAppealReviewerCommand fromRequest(UUID appealId, UUID reviewerId) {
        return new RemoveExamAppealReviewerCommand(appealId, reviewerId);
    }

    public static SubmitExamAppealReportCommand fromRequest(
            UUID appealId, SubmitExamAppealReportRequest request) {
        var items = request.items().stream()
            .map(item -> new SubmitExamAppealReportCommand.ItemReport(
                item.appealItemId(),
                item.scores().stream()
                    .map(score -> new SubmitExamAppealReportCommand.CriterionScoreItem(
                        score.criterionId(),
                        score.score(),
                        score.rationale()
                    ))
                    .toList(),
                item.note()
            ))
            .toList();
        return new SubmitExamAppealReportCommand(appealId, items);
    }

    public static PublishExamAppealCommand fromRequest(UUID appealId, PublishExamAppealRequest request) {
        var itemScores = request.itemScores().stream()
            .map(item -> new PublishExamAppealCommand.ItemScore(item.appealItemId(), item.partScore()))
            .toList();
        return new PublishExamAppealCommand(appealId, itemScores, request.decisionNote());
    }
}
