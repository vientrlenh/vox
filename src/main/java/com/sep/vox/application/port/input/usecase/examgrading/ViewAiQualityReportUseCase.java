package com.sep.vox.application.port.input.usecase.examgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAiQualityReportQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AiQualityReportInfo;
import com.sep.vox.application.query.repository.ExamResultAuditQueryRepository;

/**
 * "AI chấm lệch bao nhiêu", tính từ chính kết quả hậu kiểm đã có.
 *
 * <p>Phạm vi luôn khoá trong trường của người gọi — số liệu chất lượng chấm của một
 * trường không phải thứ trường khác được xem.
 */
@Service
public class ViewAiQualityReportUseCase implements IUseCase<ViewAiQualityReportQuery, AiQualityReportInfo> {

    private final ExamResultAuditQueryRepository examResultAuditQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewAiQualityReportUseCase(
            ExamResultAuditQueryRepository examResultAuditQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examResultAuditQueryRepository = examResultAuditQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public AiQualityReportInfo execute(ViewAiQualityReportQuery input) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var schoolId = examGradingAccessService.requireCurrentSchoolId(currentUserId);
        examGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);
        return examResultAuditQueryRepository.aiQualityReport(schoolId, input.examId());
    }
}
