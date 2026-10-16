package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.BulkFinalizePreviewInfo;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

/**
 * Xem trước hậu quả của việc chốt sổ, KHÔNG ghi gì.
 *
 * <p>Bắt buộc phải có bước này vì chốt sổ là thao tác một chiều trên cả kỳ thi: admin
 * cần thấy còn bao nhiêu bài chưa ai chấm và bao nhiêu đơn phúc khảo đang dở trước khi
 * quyết định công bố theo điểm AI.
 *
 * <p>Rộng hơn người được chốt sổ MỘT bậc, và đó là chủ ý: quyền đọc ở đây là
 * {@code authorizeSchoolAdminOrAssessmentChair} (chủ tịch của kỳ thi, mọi loại) trong khi
 * {@link BulkFinalizeAssessmentResultsUseCase} vẫn chỉ nhận school admin hoặc chủ tịch bài trên
 * lớp. Chủ tịch kỳ thi tập trung bấm được nút công bố kết quả nhưng không chốt sổ được,
 * nên nếu không đọc được đúng bảng này thì lần bấm nào hỏng họ cũng chỉ nhận một con số
 * "còn N kết quả chưa RELEASED" mà không có đường nào tra ra N bài đó là bài nào. Con số
 * ở đây trả lời đúng câu đó — và vẫn đóng phạm vi trong một kỳ thi.
 */
@Service
public class PreviewBulkFinalizeUseCase implements IUseCase<UUID, BulkFinalizePreviewInfo> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public PreviewBulkFinalizeUseCase(
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentRepository assessmentRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public BulkFinalizePreviewInfo execute(UUID assessmentId) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        var assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra."));
        assessmentGradingAccessService.authorizeSchoolAdminOrAssessmentChair(
            assessment.getSchoolId(), assessment.getId(), currentUserId);
        return assessmentGradingQueryRepository.previewBulkFinalize(assessment.getSchoolId(), assessmentId);
    }
}
