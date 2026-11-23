package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAiQualityReportQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AiQualityReportInfo;
import com.sep.vox.application.query.repository.AssessmentResultAuditQueryRepository;

/**
 * "AI chấm lệch bao nhiêu", tính từ chính kết quả hậu kiểm đã có.
 *
 * <p>Phạm vi luôn khoá trong trường của người gọi — số liệu chất lượng chấm của một
 * trường không phải thứ trường khác được xem.
 */
@Service
@RequiredArgsConstructor
public class ViewAiQualityReportUseCase implements IUseCase<ViewAiQualityReportQuery, AiQualityReportInfo> {

    private final AssessmentResultAuditQueryRepository assessmentResultAuditQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional(readOnly = true)
    public AiQualityReportInfo execute(ViewAiQualityReportQuery input) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        UUID schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        assessmentGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);
        return assessmentResultAuditQueryRepository.aiQualityReport(schoolId, input.assessmentId());
    }
}
