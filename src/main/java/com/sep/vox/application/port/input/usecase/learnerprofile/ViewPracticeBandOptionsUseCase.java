package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.PracticeBandOption;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveInteger;


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
        return execute((UUID) null);
    }

    /**
     * Thang bậc của MỘT khung cụ thể, sau khi học sinh đã chọn khung ở ô phía trên.
     *
     * <p>{@code null} = giữ hành vi cũ: lấy khung đang hiệu lực toàn hệ. Client cũ không gửi
     * tham số vẫn chạy y như trước.
     */
    @Transactional(readOnly = true)
    public List<PracticeBandOption> execute(UUID frameworkVersionId) {
        // Danh sách bậc là khái niệm TOÀN HỆ, không phụ thuộc học sinh nào -- nên không cần
        // studentId ở đây. Việc chặn người lạ do @PreAuthorize("hasRole('STUDENT')") ở
        // PracticeController lo, không phải bằng cách gọi UserContextPort rồi vứt kết quả.
        var ladder = frameworkVersionId == null
            ? enrichmentService.frameworkBandLadder()
            : enrichmentService.frameworkBandLadder(frameworkVersionId);
        return ladder.stream()
            .map(band -> new PracticeBandOption(
                band.getId(),
                Code.valueOf(band.getCode()),
                band.getLabel(),
                band.getDescription(),
                PositiveInteger.valueOf(band.getOrder())
            ))
            .toList();
    }
}
