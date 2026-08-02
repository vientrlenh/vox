package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
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

    public record RankSignal(int base, int scoreAdjustment) {
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
        return new RankSignal(base, adjustment);
    }

    /** Phần topic-specific -- 1 query rẻ mỗi topic, không lặp lại phần student-level ở trên. */
    public int rankForTopic(UUID studentId, UUID topicId, RankSignal signal) {
        var diagnoses = practiceSessionQueryRepository.findLastAbandonDiagnosis(studentId, topicId);
        var adjustment = !diagnoses.isEmpty() && BAD_ABANDON_DIAGNOSES.contains(diagnoses.get(0))
            ? -1
            : signal.scoreAdjustment();
        return Math.max(1, Math.min(6, signal.base() + adjustment));
    }

    public String levelLabel(int rank) {
        if (rank <= 2) {
            return "BEGINNER";
        }
        if (rank <= 4) {
            return "INTERMEDIATE";
        }
        return "ADVANCED";
    }

    private int policyBandOrder(UUID studentId) {
        var values = learnerProfileRepository.findPolicyTargetBandOrder(studentId);
        return values.isEmpty() ? 3 : values.get(0);
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
        if (band <= 2) {
            return 720;
        }
        return band == 3 ? 900 : 1200;
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
