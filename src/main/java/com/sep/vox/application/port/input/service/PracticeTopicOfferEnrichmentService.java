package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.repository.personalization.LearnerProfileRepository;
import com.sep.vox.domain.repository.personalization.LearnerWeaknessSnapshotRepository;

/**
 * Minutes, trần thời lượng và focusTags cho topic offer -- gộp 1 chỗ vì có nhiều caller
 * (BuildPracticePaperUseCase, ViewPracticeTopicOffersUseCase, SearchPracticeTopicsUseCase,
 * PickRandomTopicUseCase, TopicSuggestionService).
 *
 * <p>KHÔNG còn tính bậc cho học sinh. Trước đây lớp này suy ra bậc từ ba nguồn (bậc đo được
 * từ bài chấm, EMA hiệu năng, lần bỏ dở gần nhất theo chủ đề) -- tức lấy độ khó của CÂU HỎI
 * gán thành trình độ của NGƯỜI HỌC, một tuyên bố hệ thống không chứng minh được. Giờ học
 * sinh tự chọn bậc muốn luyện mỗi phiên, và bậc đó chỉ còn nghĩa "độ khó tôi muốn hôm nay".
 */
@Service
public class PracticeTopicOfferEnrichmentService {

    /** Dùng khi học sinh chưa gắn với chính sách chấm nào -- giữ nguyên hành vi cũ cho dữ
     * liệu chưa cấu hình, không phải vì hệ thống mặc định là VSTEP. */
    private static final int DEFAULT_BAND_COUNT = 6;

    /** Chỉ dùng khi bậc của phiên đã bị xoá khỏi khung -- xem {@link #bandOrder}. */
    private static final int DEFAULT_BAND_ORDER = 3;

    private static final Map<String, String> CRITERION_LABELS = Map.of(
        "GRAMMAR", "Ngữ pháp",
        "VOCABULARY", "Từ vựng",
        "COHERENCE", "Mạch lạc",
        "PRONUNCIATION", "Phát âm",
        "FLUENCY", "Trôi chảy"
    );

    private final LearnerProfileRepository learnerProfileRepository;
    private final LearnerWeaknessSnapshotRepository weaknessRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;

    public PracticeTopicOfferEnrichmentService(
            LearnerProfileRepository learnerProfileRepository,
            LearnerWeaknessSnapshotRepository weaknessRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository) {
        this.learnerProfileRepository = learnerProfileRepository;
        this.weaknessRepository = weaknessRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
    }

    /**
     * Thứ tự của một bậc trên thang. Bậc đã bị xoá khỏi khung thì lùi về giữa thang -- thà
     * hỏi hơi lệch còn hơn làm chết một phiên đang chạy vì dữ liệu cấu hình đổi sau lưng.
     */
    public int bandOrder(UUID bandId) {
        if (bandId == null) {
            return DEFAULT_BAND_ORDER;
        }
        return frameworkResultBandRepository.findById(bandId)
            .map(FrameworkResultBand::getOrder)
            .orElse(DEFAULT_BAND_ORDER);
    }

    /**
     * Số bậc của thang năng lực đang áp cho học sinh. Mặc định {@value #DEFAULT_BAND_COUNT}
     * khi chưa có chính sách chấm nào -- không phải vì VSTEP, mà vì đó là giá trị an toàn
     * giữ nguyên hành vi cũ cho dữ liệu chưa cấu hình.
     */
    public int frameworkBandCount(UUID studentId) {
        var values = learnerProfileRepository.findFrameworkBandCount(studentId);
        return values.isEmpty() || values.get(0) == null || values.get(0) < 1
            ? DEFAULT_BAND_COUNT
            : values.get(0);
    }

    /** Cả thang bậc kèm mô tả, để gửi xuống Python dựng ladder trong prompt chấm câu hỏi.
     * Rỗng thì Python tự lùi về ladder mặc định của nó. */
    public List<FrameworkResultBand> frameworkBandLadder(UUID studentId) {
        return learnerProfileRepository.findFrameworkBandLadder(studentId);
    }

    /**
     * Trần độ dài MỘT phiên theo bậc ĐANG LUYỆN (thiết kế gói 6 mục 4.1: bậc 1-2 → 720s,
     * bậc 3 → 900s, bậc 4+ → 1200s).
     *
     * Khác bản chất với hạn mức subscription ({@link #minutesForStudent}): cái kia là giới hạn
     * THƯƠNG MẠI (trường mua bao nhiêu), cái này là giới hạn SƯ PHẠM (bậc này ngồi luyện liên
     * tục bao lâu là hợp lý). Phải áp CẢ HAI, lấy cái nhỏ hơn -- chỉ có hạn mức gói thì học
     * sinh mới bắt đầu vẫn có thể bị đẩy vào phiên 20 phút.
     *
     * <p>Tham số là bậc học sinh CHỌN cho phiên này, không phải bậc hệ thống đoán -- đó là
     * toàn bộ khác biệt so với bản trước.
     */
    public int sessionSecondsCapForBand(int bandOrder, int bandCount) {
        // Chia theo TỈ LỆ vị trí trên thang, không cắt cứng ở bậc 2/3: với 6 bậc cho ra đúng
        // 720/720/900/1200/1200/1200 như trước, với thang khác thì tự giãn theo.
        var ratio = (double) bandOrder / Math.max(1, bandCount);
        if (ratio <= 1.0 / 3) {
            return 720;
        }
        return ratio <= 1.0 / 2 ? 900 : 1200;
    }

    /**
     * Ngân sách nói THẬT SỰ của một phiên: chỗ hẹp hơn giữa hạn mức gói và trần bậc.
     *
     * Trước đây công thức này viết thẳng trong {@code ResolveNextPracticeQuestionClaimService}
     * -- nơi duy nhất cần nó. Giờ màn hình luyện cũng phải hiện "đã nói / ngân sách" nên có hai
     * nơi cần cùng con số; để mỗi nơi tự tính lại là mở đường cho chúng lệch nhau, rồi thanh
     * tiến độ trên máy học sinh nói một đằng còn phiên dừng một nẻo.
     */
    public int sessionBudgetSeconds(UUID studentId, int bandOrder) {
        return Math.min(
            minutesForStudent(studentId) * 60,
            sessionSecondsCapForBand(bandOrder, frameworkBandCount(studentId))
        );
    }

    /** Số phút mỗi lượt luyện theo đúng gói subscription đang hoạt động -- 0 nếu không có gói. */
    public int minutesForStudent(UUID studentId) {
        var minutes = schoolSubscriptionRepository.findMaxTimePerAttemptMinForUser(studentId);
        return minutes == null ? 0 : minutes;
    }

    /** 1-2 tiêu chí yếu nhất, đã map sang nhãn tiếng Việt để hiển thị trực tiếp lên chip. */
    public List<String> focusTagsForStudent(UUID studentId) {
        return weaknessRepository.findFocusCriterionCodesOrderedByWeakness(studentId).stream()
            .limit(2)
            .map(code -> CRITERION_LABELS.getOrDefault(code, code))
            .toList();
    }

    /** Mã tiêu chí yếu nhất của học sinh, hoặc null nếu chưa có dữ liệu weakness nào. */
    public String weakestCriterion(UUID studentId) {
        return weaknessRepository.findFocusCriterionCodesOrderedByWeakness(studentId).stream()
            .findFirst()
            .orElse(null);
    }

    public String criterionLabel(String code) {
        return CRITERION_LABELS.getOrDefault(code, code);
    }
}
