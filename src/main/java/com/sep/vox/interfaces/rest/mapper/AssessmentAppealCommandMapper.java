package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.ApproveAssessmentAppealCommand;
import com.sep.vox.application.port.input.command.AssignAssessmentAppealReviewerCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentAppealCommand;
import com.sep.vox.application.port.input.command.RejectAssessmentAppealCommand;
import com.sep.vox.interfaces.rest.dto.request.ApproveAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.AssignAssessmentAppealReviewerRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentAppealRequest;
import com.sep.vox.interfaces.rest.dto.request.RejectAssessmentAppealRequest;

public final class AssessmentAppealCommandMapper {

    private AssessmentAppealCommandMapper() {}

    public static CreateAssessmentAppealCommand fromRequest(CreateAssessmentAppealRequest request) {
        return new CreateAssessmentAppealCommand(
            request.candidateResultId(),
            request.paperItemIds(),
            request.reason(),
            request.notes()
        );
    }

    public static ApproveAssessmentAppealCommand fromRequest(UUID appealId, ApproveAssessmentAppealRequest request) {
        return new ApproveAssessmentAppealCommand(
            appealId, 
            DateMapper.toInstant(request.deadline())
        );
    }

    public static RejectAssessmentAppealCommand fromRequest(UUID appealId, RejectAssessmentAppealRequest request) {
        return new RejectAssessmentAppealCommand(appealId, request.reason());
    }

    public static AssignAssessmentAppealReviewerCommand fromRequest(
            UUID appealId, AssignAssessmentAppealReviewerRequest request) {
        return new AssignAssessmentAppealReviewerCommand(
            appealId, request.reviewerId(), request.overrideReason(), request.deadlineAt());
    }
}
