package com.sep.vox.application.port.input.usecase.examgrading;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.BulkFinalizePreviewInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.repository.ExamRepository;

/**
 * Xem trước hậu quả của việc chốt sổ, KHÔNG ghi gì.
 *
 * <p>Bắt buộc phải có bước này vì chốt sổ là thao tác một chiều trên cả kỳ thi: admin
 * cần thấy còn bao nhiêu bài chưa ai chấm và bao nhiêu đơn phúc khảo đang dở trước khi
 * quyết định công bố theo điểm AI.
 */
@Service
public class PreviewBulkFinalizeUseCase implements IUseCase<UUID, BulkFinalizePreviewInfo> {

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamRepository examRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public PreviewBulkFinalizeUseCase(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamRepository examRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examRepository = examRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public BulkFinalizePreviewInfo execute(UUID examId) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var exam = examRepository.findById(examId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra."));
        // Bài kiểm tra trên lớp: nhà trường không chấm, nên nếu chỉ school admin chốt
        // sổ được thì giáo viên kẹt cứng — công bố kết quả đòi MỌI bài phải RELEASED
        // hoặc INVALID (UpdateExamStatusUseCase.requirePublishReadiness).
        examGradingAccessService.authorizeSchoolAdminOrClassTestChair(
            exam.getSchoolId(), exam.getId(), currentUserId);
        return examGradingQueryRepository.previewBulkFinalize(exam.getSchoolId(), examId);
    }
}
