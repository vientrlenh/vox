package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ResultDecisionMethod;

public record UpdateExamCommand(
    UUID examId,
    String name,
    String description,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,
    Integer maxAttempt,
    Integer examTimeDurationSecond,
    ResultDecisionMethod resultDecisionMethod,
    Boolean requiresOtp,
    /** null = giữ nguyên cấu hình giám sát; danh sách rỗng = tắt giám sát. */
    List<String> requiredStreamTypes,
    String streamTypePermission
) {
}
