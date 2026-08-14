package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.PracticeFrameworkOption;

/**
 * Các khung đánh giá học sinh có thể chọn trước khi chọn bậc luyện tập.
 *
 * <p>Mỗi khung chỉ hiện BẢN ĐÃ BAN HÀNH MỚI NHẤT, do server tự chọn -- học sinh không phải
 * biết tới khái niệm phiên bản. Bản nháp hoặc bản hết hiệu lực không lọt ra ngoài, vì truy vấn
 * dùng đúng bộ điều kiện với {@code findActiveVersionId}.
 *
 * <p>Danh sách này là khái niệm TOÀN HỆ, không phụ thuộc học sinh nào -- cùng lý do với
 * {@link ViewPracticeBandOptionsUseCase}: chặn người lạ là việc của {@code @PreAuthorize} ở
 * controller, không phải bằng cách nhận studentId rồi bỏ đi.
 */
@Service
public class ViewPracticeFrameworkOptionsUseCase implements IUseCase<Void, List<PracticeFrameworkOption>> {

    private final PracticeTopicOfferEnrichmentService enrichmentService;

    public ViewPracticeFrameworkOptionsUseCase(PracticeTopicOfferEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeFrameworkOption> execute(Void input) {
        return enrichmentService.activeFrameworks().stream()
            .map(framework -> new PracticeFrameworkOption(
                framework.versionId(),
                framework.frameworkCode(),
                framework.frameworkName(),
                framework.frameworkDescription(),
                enrichmentService.frameworkBandCount(framework.versionId())
            ))
            .toList();
    }
}
