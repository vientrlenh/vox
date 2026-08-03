package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.repository.personalization.LearnerProfileRepository;
import com.sep.vox.domain.repository.personalization.LearnerWeaknessSnapshotRepository;
import com.sep.vox.domain.repository.personalization.PracticeItemEvaluationRepository;

/**
 * Tính rank/level, minutes và focusTags cho topic offer -- gộp 1 chỗ vì có nhiều caller
 * (BuildPracticePaperUseCase, ViewPracticeTopicOffersUseCase, SearchPracticeTopicsUseCase,
 * PickRandomTopicUseCase, TopicSuggestionService).
 */
@Service
public class PracticeTopicOfferEnrichmentService {

    private static final Set<String> BAD_ABANDON_DIAGNOSES = Set.of("TOO_HARD", "UNKNOWN");

    /** Dùng khi học sinh chưa gắn với chính sách chấm nào -- giữ nguyên hành vi cũ cho dữ
     * liệu chưa cấu hình, không phải vì hệ thống mặc định là VSTEP. */
    private static final int DEFAULT_BAND_COUNT = 6;

    private static final Map<String, String> CRITERION_LABELS = Map.of(
        "GRAMMAR", "Ngữ pháp",
        "VOCABULARY", "Từ vựng",
        "COHERENCE", "Mạch lạc",
        "PRONUNCIATION", "Phát âm",
        "FLUENCY", "Trôi chảy"
    );

    private final LearnerProfileRepository learnerProfileRepository;
    private final PracticeItemEvaluationRepository practiceItemEvaluationRepository;
    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final LearnerWeaknessSnapshotRepository weaknessRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;

    public PracticeTopicOfferEnrichmentService(
            LearnerProfileRepository learnerProfileRepository,
            PracticeItemEvaluationRepository practiceItemEvaluationRepository,
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            LearnerWeaknessSnapshotRepository weaknessRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository) {
        this.learnerProfileRepository = learnerProfileRepository;
        this.practiceItemEvaluationRepository = practiceItemEvaluationRepository;
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.weaknessRepository = weaknessRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
    }

    /** @param bandCount số bậc của thang đang áp -- nằm ở đây thay vì query lại trong
     * rankForTopic/levelLabel, vì hai hàm đó chạy MỘT LẦN MỖI CHỦ ĐỀ trong vòng lặp xếp hạng
     * (query lại là N+1). studentRankSignal vốn đã tính đúng 1 lần mỗi request. */
    public record RankSignal(int base, int scoreAdjustment, int bandCount) {
    }

    /** Phần student-level của ước lượng bậc -- tính 1 lần/request, không lặp lại theo từng topic. */
    public RankSignal studentRankSignal(UUID studentId) {
        var estimated = learnerProfileRepository.findEstimatedResultBandOrder(studentId).stream()
            .findFirst()
            .orElse(null);
        var base = estimated == null ? policyBandOrder(studentId) : estimated;
        var scores = practiceItemEvaluationRepository.findNormalizedScoresChronological(studentId);
        var adjustment = 0;
        if (!scores.isEmpty()) {
            // EMA, không phải trung bình cộng cửa sổ cứng -- cùng công thức đệ quy
            // InterestVectorService.recomputeInterest đang dùng cho interest score (mỗi điểm
            // mới góp phần ngay từ điểm đầu tiên, điểm gần đây có trọng số cao hơn tự nhiên,
            // không phải chờ đúng 3 điểm rồi mới có tác dụng).
            var performance = 0.5;
            for (var score : scores) {
                performance = 0.3 * score + 0.7 * performance;
            }
            adjustment = performance >= 0.75 ? 1 : performance <= 0.45 ? -1 : 0;
        }
        return new RankSignal(base, adjustment, frameworkBandCount(studentId));
    }

    /** Phần topic-specific -- 1 query rẻ mỗi topic, không lặp lại phần student-level ở trên. */
    public int rankForTopic(UUID studentId, UUID topicId, RankSignal signal) {
        var diagnoses = practiceSessionQueryRepository.findLastAbandonDiagnosis(studentId, topicId);
        var adjustment = !diagnoses.isEmpty() && BAD_ABANDON_DIAGNOSES.contains(diagnoses.get(0))
            ? -1
            : signal.scoreAdjustment();
        return Math.max(1, Math.min(signal.bandCount(), signal.base() + adjustment));
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
     * Nhãn mức độ hiển thị trên thẻ chủ đề. Chia thang thành 3 phần THEO TỈ LỆ thay vì cắt
     * cứng ở bậc 2/4 -- với 6 bậc cho ra đúng kết quả cũ (1,2 → BEGINNER · 3,4 →
     * INTERMEDIATE · 5,6 → ADVANCED), với 9 bậc thì tự giãn thành 1-3 / 4-6 / 7-9.
     */
    public String levelLabel(int rank, int bandCount) {
        var safeBandCount = Math.max(1, bandCount);
        var ratio = (double) rank / safeBandCount;
        if (ratio <= 1.0 / 3) {
            return "BEGINNER";
        }
        if (ratio <= 2.0 / 3) {
            return "INTERMEDIATE";
        }
        return "ADVANCED";
    }

    private int policyBandOrder(UUID studentId) {
        var values = learnerProfileRepository.findPolicyTargetBandOrder(studentId);
        // Chưa có chính sách -> lấy GIỮA thang, không phải hằng số 3: với 6 bậc vẫn ra 3 như
        // trước, với 9 bậc ra 5 (đúng nghĩa "giữa") thay vì lệch xuống dưới.
        return values.isEmpty()
            ? (frameworkBandCount(studentId) + 1) / 2
            : values.get(0);
    }

    /**
     * Trần độ dài MỘT phiên theo bậc năng lực (thiết kế gói 6 mục 4.1: BAC_1-2 → 720s,
     * BAC_3 → 900s, BAC_4+ → 1200s).
     *
     * Khác bản chất với hạn mức subscription ({@link #minutesForStudent}): cái kia là giới hạn
     * THƯƠNG MẠI (trường mua bao nhiêu), cái này là giới hạn SƯ PHẠM (bậc này ngồi luyện liên
     * tục bao lâu là hợp lý). Phải áp CẢ HAI, lấy cái nhỏ hơn -- chỉ có hạn mức gói thì học
     * sinh mới bắt đầu vẫn có thể bị đẩy vào phiên 20 phút.
     */
    public int sessionSecondsCapForStudent(UUID studentId) {
        var band = studentRankSignal(studentId).base();
        // Chia theo TỈ LỆ vị trí trên thang, không cắt cứng ở bậc 2/3: với 6 bậc cho ra đúng
        // 720/720/900/1200/1200/1200 như trước, với thang khác thì tự giãn theo.
        var ratio = (double) band / Math.max(1, frameworkBandCount(studentId));
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
    public int sessionBudgetSecondsForStudent(UUID studentId) {
        return Math.min(minutesForStudent(studentId) * 60, sessionSecondsCapForStudent(studentId));
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
