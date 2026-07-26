package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ApproveExamAppealCommand;
import com.sep.vox.application.port.input.command.AssignExamAppealReviewerCommand;
import com.sep.vox.application.port.input.command.CreateExamAppealCommand;
import com.sep.vox.application.port.input.command.RejectExamAppealCommand;
import com.sep.vox.interfaces.rest.dto.request.ApproveExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.AssignExamAppealReviewerRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateExamAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.RejectExamAppealRequest;

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
        return new ApproveExamAppealCommand(appealId, request.deadline());
    }

    public static RejectExamAppealCommand fromRequest(UUID appealId, RejectExamAppealRequest request) {
        return new RejectExamAppealCommand(appealId, request.reason());
    }

    public static AssignExamAppealReviewerCommand fromRequest(
            UUID appealId, AssignExamAppealReviewerRequest request) {
        return new AssignExamAppealReviewerCommand(
            appealId, request.reviewerId(), request.overrideReason(), request.deadlineAt());
    }
}
