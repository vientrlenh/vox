package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.PracticeBandOption;

/**
 * Thang bậc của khung đang áp, cho ô chọn độ khó trước mỗi phiên luyện.
 *
 * <p>Trước đây học sinh không chọn gì: hệ thống suy ra bậc từ bài chấm trên lớp cộng hiệu năng
 * luyện gần đây, rồi ra đề theo bậc đó. Suy như vậy là lấy độ khó của CÂU HỎI gán thành trình
 * độ của NGƯỜI HỌC -- một tuyên bố hệ thống không có cơ sở để đưa ra. Giờ chỉ còn liệt kê các
 * bậc kèm mô tả để học sinh tự chọn.
 */
@Service
public class ViewPracticeBandOptionsUseCase implements IUseCase<Void, List<PracticeBandOption>> {

    private final PracticeTopicOfferEnrichmentService enrichmentService;

    public ViewPracticeBandOptionsUseCase(
            PracticeTopicOfferEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeBandOption> execute(Void input) {
        // Danh sách bậc là khái niệm TOÀN HỆ, không phụ thuộc học sinh nào -- nên không cần
        // studentId ở đây. Việc chặn người lạ do @PreAuthorize("hasRole('STUDENT')") ở
        // PracticeController lo, không phải bằng cách gọi UserContextPort rồi vứt kết quả.
        return enrichmentService.frameworkBandLadder().stream()
            .map(band -> new PracticeBandOption(
                band.getId(),
                band.getCode(),
                band.getLabel(),
                band.getDescription(),
                band.getOrder()
            ))
            .toList();
    }
}
