package com.sep.vox.infrastructure.persistence.query;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.LearnerProfileInfo;
import com.sep.vox.application.query.repository.LearnerProfileQueryRepository;
import com.sep.vox.infrastructure.persistence.entity.FrameworkResultBandJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataLearnerProfileRepository;

@Repository
public class JpaLearnerProfileQueryRepository
        implements LearnerProfileQueryRepository {

    private final SpringDataLearnerProfileRepository repository;

    /** Rỗng = tự chọn khung đang hoạt động. Cùng thuộc tính mà
     * {@code PracticeTopicOfferEnrichmentService} dùng -- hai nơi phải ghim cùng một khung. */
    private final String practiceFrameworkVersionCode;

    public JpaLearnerProfileQueryRepository(
            SpringDataLearnerProfileRepository repository,
            @Value("${app.practice.framework-version-code:}") String practiceFrameworkVersionCode) {
        this.repository = repository;
        this.practiceFrameworkVersionCode =
            practiceFrameworkVersionCode == null || practiceFrameworkVersionCode.isBlank()
                ? null
                : practiceFrameworkVersionCode.trim();
    }

    /**
     * Bậc mục tiêu nay lấy từ KHUNG đang hoạt động, không còn tra {@code assessment_policies}
     * theo lớp của học sinh -- và thiếu thì trả null chứ không ném.
     *
     * <p>Đường cũ sai ở hai tầng chồng lên nhau.
     *
     * <p><b>Sai phạm vi.</b> Đây là mảnh cuối cùng của luyện tập còn dính vào phạm vi trường, dù
     * phần còn lại đã chuyển sang khung toàn hệ từ V13 (xem
     * {@code PracticeTopicOfferEnrichmentService.frameworkBandCount/frameworkBandLadder}). Giữ lại
     * nghĩa là số bậc lấy từ khung mà bậc gợi ý lại lấy từ chính sách -- hai nguồn có thể trỏ vào
     * hai thang khác nhau mà không ai phát hiện.
     *
     * <p><b>Ném sai chỗ.</b> Bốn use case gọi hàm này đều theo dạng "GHI xong rồi ĐỌC lại để dựng
     * phản hồi", và cả bốn đều {@code @Transactional}. Ném ở bước đọc là rollback luôn bước ghi.
     * Đo thực tế: học sinh THPT Trần Thông nộp quiz sở thích xong, {@code interest_quiz_item} còn
     * 7 dòng nhưng {@code learner_profile} 0 dòng. Nguyên nhân chỉ là chính sách của trường trỏ
     * vào một niên khoá không có lớp nào -- một cấu hình sót của quản trị nuốt mất bài làm của
     * học sinh, và kéo theo cả sinh chủ đề, xem lịch, xem kết quả đứng theo.
     *
     * <p>Thứ thiếu chỉ là một NHÃN: mã và tên bậc gợi ý sẵn ở màn chọn độ khó. Học sinh vẫn tự
     * chọn được bậc, nên null ở đây làm giao diện thiếu gợi ý chứ không làm hỏng việc gì.
     */
    @Override
    public LearnerProfileInfo findCurrent(UUID studentId) {
        var current = repository
            .findByStudentId(studentId)
            .orElse(null);
        var target = repository.findDefaultTargetBand(practiceFrameworkVersionCode)
            .stream()
            .findFirst()
            .orElse(null);
        return new LearnerProfileInfo(
            current == null || current.getGoalType() == null
                ? "ABILITY_IMPROVEMENT"
                : current.getGoalType(),
            target == null ? null : target.getCode(),
            target == null ? null : label(target),
            current == null || current.isAutoUpdateInterest(),
            current == null || current.getQuizCompletedAt() == null
                ? null
                : current.getQuizCompletedAt().toString()
        );
    }

    /** Khung chưa điền nhãn thì lấy mã làm nhãn -- thà hiện "B2" còn hơn hiện ô trống. */
    private static String label(FrameworkResultBandJpaEntity band) {
        return band.getLabel() == null || band.getLabel().isBlank()
            ? band.getCode()
            : band.getLabel();
    }
}
