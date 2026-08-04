package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;

/**
 * Kiểm tra bộ tiêu chí đánh giá gắn vào bài kiểm tra.
 *
 * <p>Bài không gắn policy hợp lệ thì {@code ExamSessionResultCalculator} ném ngay khi tính kết quả,
 * tức là không sinh được {@code ExamCandidateResult} nào — mà đó mới là thứ phân công chấm trỏ vào.
 * Luật này giống hệt nhau cho bài trên lớp và kỳ thi tập trung nên gom về một chỗ, chỉ khác ở chỗ
 * policy có bắt buộc ngay lúc tạo hay không.
 *
 * <p>Policy hệ thống ({@code schoolId = null}) dùng được cho mọi trường.
 */
@Service
public class ExamAssessmentPolicyValidator {

    private final AssessmentPolicyRepository assessmentPolicyRepository;

    public ExamAssessmentPolicyValidator(AssessmentPolicyRepository assessmentPolicyRepository) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
    }

    /**
     * Bắt buộc phải có policy hợp lệ. Bài trên lớp chốt policy ngay lúc tạo, không để trôi sang bước
     * sửa — giáo viên tạo xong là chấm được luôn.
     */
    public void requirePublishedInSchool(UUID assessmentPolicyId, UUID schoolId) {
        if (assessmentPolicyId == null) {
            throw new IllegalArgumentException("Bộ tiêu chí đánh giá là bắt buộc");
        }
        validate(assessmentPolicyId, schoolId);
    }

    /**
     * Chỉ kiểm khi có giá trị. Kỳ thi tập trung tạo "vỏ" trước rồi mới chọn policy ở bước sửa, nên bỏ
     * trống lúc tạo là hợp lệ — nhưng đã gửi lên thì phải đúng. Việc bắt buộc có policy được dời sang
     * lúc SCHEDULE (xem {@code UpdateExamStatusUseCase}).
     */
    public void validateIfPresent(UUID assessmentPolicyId, UUID schoolId) {
        if (assessmentPolicyId == null) {
            return;
        }
        validate(assessmentPolicyId, schoolId);
    }

    private void validate(UUID assessmentPolicyId, UUID schoolId) {
        var policy = assessmentPolicyRepository.findById(assessmentPolicyId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ tiêu chí đánh giá"));
        if (policy.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được dùng bộ tiêu chí đã xuất bản");
        }
        if (policy.getSchoolId() != null && !policy.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Bộ tiêu chí không thuộc trường của bạn");
        }
    }
}
