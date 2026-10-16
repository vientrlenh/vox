package com.sep.vox.interfaces.rest.dto.request;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AcceptRubricCriterionImportCommand;
import com.sep.vox.application.port.input.command.AcceptRubricResultBandImportCommand;
import com.sep.vox.application.port.input.command.AcceptSchoolAssessmentPolicyImportCommand;
import com.sep.vox.application.port.input.command.AcceptSchoolRubricVersionImportCommand;
import com.sep.vox.application.port.input.command.AcceptSystemAssessmentPolicyImportCommand;
import com.sep.vox.application.port.input.command.AcceptSystemRubricVersionImportCommand;

import jakarta.validation.constraints.NotEmpty;

/**
 * Thân request xác nhận ghép cột, dùng chung cho các luồng import mới.
 *
 * <p>Khoá là tiêu đề cột trong file, giá trị là tên trường hệ thống.
 * {@code AcceptQuestionImportRequest} có nội dung y hệt nhưng giữ nguyên — không đổi kiểu tham số
 * của một endpoint đang chạy chỉ để gộp tên.
 */
public record AcceptImportRequest(
    @NotEmpty(message = "Mapping import không được để trống")
    Map<String, String> confirmedMapping
) {

    public static AcceptSystemAssessmentPolicyImportCommand toAcceptSystemAssessmentPolicyImportCommand(UUID sessionId, AcceptImportRequest request) {
        return new AcceptSystemAssessmentPolicyImportCommand(sessionId, request.confirmedMapping());
    }

    public static AcceptSchoolAssessmentPolicyImportCommand toAcceptSchoolAssessmentPolicyImportCommand(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptSchoolAssessmentPolicyImportCommand(schoolId, sessionId, request.confirmedMapping());
    }

    public static AcceptRubricCriterionImportCommand toAcceptRubricCriterionImportCommand(UUID sessionId, AcceptImportRequest request) {
        return new AcceptRubricCriterionImportCommand(null, sessionId, request.confirmedMapping());
    }

    public static AcceptRubricCriterionImportCommand toAcceptRubricCriterionImportCommand(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptRubricCriterionImportCommand(schoolId, sessionId, request.confirmedMapping());
    }

    public static AcceptRubricResultBandImportCommand toAcceptRubricResultBandImportCommand(UUID sessionId, AcceptImportRequest request) {
        return new AcceptRubricResultBandImportCommand(null, sessionId, request.confirmedMapping());
    }

    public static AcceptRubricResultBandImportCommand toAcceptRubricResultBandImportCommand(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptRubricResultBandImportCommand(schoolId, sessionId, request.confirmedMapping());
    }

    public static AcceptSystemRubricVersionImportCommand toAcceptSystemRubricVersionImportCommand(UUID sessionId, AcceptImportRequest request) {
        return new AcceptSystemRubricVersionImportCommand(sessionId, request.confirmedMapping());
    }

    public static AcceptSchoolRubricVersionImportCommand toAcceptSchoolRubricVersionImportCommand(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptSchoolRubricVersionImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
