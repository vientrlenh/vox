package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewPracticeTopicOffersQuery;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.service.TopicSuggestionService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.QuestionTopicInfo;
import com.sep.vox.application.query.repository.PracticeTopicQueryRepository;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.service.personalization.TensePolicy;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.personalization.LearnerProfileRepository;
import com.sep.vox.domain.repository.personalization.PracticePaperRepository;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;

@Service
public class ViewPracticeTopicOffersUseCase implements IUseCase<ViewPracticeTopicOffersQuery, List<PracticeTopicOffer>> {

    private static final String EXAM_TOPIC_INTEREST_DIMENSION = "ACADEMIC_EXAM";
    private static final String EXAM_TOPIC_CURRICULUM_GROUP = "EXAM_BANK";

    private final PracticeTopicQueryRepository practiceTopicQueryRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final PracticePaperRepository practicePaperRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final UserContextPort userContextPort;
    private final PracticeTopicRepository practiceTopicRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;

    public ViewPracticeTopicOffersUseCase(
            PracticeTopicQueryRepository practiceTopicQueryRepository,
            LearnerProfileRepository learnerProfileRepository,
            PracticePaperRepository practicePaperRepository,
            PracticeTopicOfferEnrichmentService enrichmentService,
            UserContextPort userContextPort,
            PracticeTopicRepository practiceTopicRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolClassUserRepository schoolClassUserRepository) {
        this.practiceTopicQueryRepository = practiceTopicQueryRepository;
        this.learnerProfileRepository = learnerProfileRepository;
        this.practicePaperRepository = practicePaperRepository;
        this.enrichmentService = enrichmentService;
        this.userContextPort = userContextPort;
        this.practiceTopicRepository = practiceTopicRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
    }

    @Override
    @Transactional
    public List<PracticeTopicOffer> execute(ViewPracticeTopicOffersQuery input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var excluded = new HashSet<>(
            input.excludeTopicIds() == null ? List.<UUID>of() : input.excludeTopicIds()
        );
        var goal = currentGoal(studentId);
        // `bucket` không còn phân nhánh gì: enum chỉ còn FOR_YOU (xem practice-planning.graphqls).
        var ranked = ("EXAM_PREP".equals(goal)
                ? examTopicRanked(studentId)
                : classicRanked(studentId, goal))
            .stream()
            .filter(topic -> !excluded.contains(topic.id()))
            .sorted(Comparator.comparingDouble((RankedTopic topic) -> topic.score()).reversed())
            .toList();
        var selected = new ArrayList<>(ranked.subList(0, Math.min(4, ranked.size())));
        var epsilon = input.round() >= 2 ? 0.30 : 0.10;
        if (selected.size() == 4
                && ranked.size() > 10
                && explorationAllowed(studentId)
                && ThreadLocalRandom.current().nextDouble() < epsilon) {
            var exploratory = ranked.subList(10, ranked.size());
            selected.set(
                selected.size() - 1,
                exploratory.get(ThreadLocalRandom.current().nextInt(exploratory.size()))
            );
        }
        // KHÔNG xáo thứ tự: client hiện `matchPercent` lấy đúng từ `score` vừa dùng để xếp
        // hạng, nên xáo xong thì thẻ #1 không còn là thẻ khớp cao nhất -- người dùng thấy
        // danh sách tự mâu thuẫn với chính con số nó in ra, và mỗi lần mở lại một thứ tự.
        //
        // Đa dạng vẫn còn nguyên, nhưng ở chỗ đúng của nó: slot epsilon phía trên thay thẻ
        // CUỐI bằng một chủ đề ngoài top 10. Vì chủ đề đó xếp hạng thấp hơn nên nó vẫn nằm
        // cuối danh sách, thứ tự theo % khớp giữ được tính đơn điệu.
        var minutes = enrichmentService.minutesForStudent(studentId);
        return selected.stream()
            .map(topic -> {
                var rationale = rationaleFor(topic);
                return new PracticeTopicOffer(
                    topic.id(),
                    topic.name(),
                    topic.dimension(),
                    topic.saved(),
                    clampPercent(topic.score()),
                    minutes,
                    rationale,
                    List.of(rationale)
                );
            })
            .toList();
    }

    /** Số thẻ đứng đầu bảng xếp hạng được coi là "trong tầm ngắm" -- chủ đề ngoài ngưỡng này
     * chỉ có thể lọt vào lô chào qua ε-greedy (xem execute()). */
    private static final int TOP_RANK_WINDOW = 10;

    /**
     * Chủ đề này có nằm NGOÀI top-{@value #TOP_RANK_WINDOW} theo Final(t) không.
     *
     * Dùng để suy ra phiên có phải do thăm dò hệ thống đẩy hay không, thay vì phải NHỚ slot
     * epsilon nào đã chào cho ai: nhớ thì cần một map sống mãi trong RAM (rò theo số học sinh ×
     * số chủ đề) hoặc phải thêm TTL/LRU với đủ thứ bug đi kèm (mất state sau restart, evict
     * nhầm lúc học sinh còn đang chọn). Xếp hạng vốn tất định nên tính lại rẻ hơn và không sai.
     */
    @Transactional(readOnly = true)
    public boolean isOutsideTopRanked(UUID studentId, UUID topicId) {
        var goal = currentGoal(studentId);
        if ("EXAM_PREP".equals(goal)) {
            // Đường EXAM_PREP không chạy ε-greedy (chỉ bucket FOR_YOU của pool AI-sinh mới có).
            return false;
        }
        var ranked = classicRanked(studentId, goal).stream()
            .sorted(Comparator.comparingDouble((RankedTopic topic) -> topic.score()).reversed())
            .toList();
        var position = -1;
        for (var index = 0; index < ranked.size(); index++) {
            if (ranked.get(index).id().equals(topicId)) {
                position = index;
                break;
            }
        }
        // Không tìm thấy (chủ đề vừa bị tắt/đổi) -> không dám kết luận là thăm dò.
        return position >= TOP_RANK_WINDOW;
    }

    private static int clampPercent(double score) {
        return Math.max(0, Math.min(100, (int) Math.round(score * 100)));
    }

    private static String rationaleFor(RankedTopic topic) {
        if (topic.fromSchool()) {
            return "Khớp chương trình học của bạn";
        }
        return "Gợi ý cho bạn";
    }

    
    private static double scoreFor(String goal, double interest, double curriculum) {
        return "EXAM_PREP".equals(goal)
            ? 0.50 * interest + 0.50 * curriculum
            : 0.80 * interest + 0.20 * curriculum;
    }

    private List<RankedTopic> classicRanked(UUID studentId, String goal) {
        var rows = practiceTopicQueryRepository.findRankedTopics(studentId, goal);
        return rows.stream()
            .map(row -> {
                var gamma = (double) row.getMentions() / (row.getMentions() + 2);
                var interest = gamma * row.getTopicScore() + (1 - gamma) * row.getDimensionScore();
                var curriculum = "IN_GDPT2018".equals(row.getCurriculumGroup()) ? 1.0 : 0.3;
                var base = scoreFor(goal, interest, curriculum);
                return new RankedTopic(
                    row.getId(),
                    row.getName(),
                    row.getInterestDimension(),
                    row.getSavedByMe(),
                    base * (1 - 0.4 * row.getRecency()),
                    interest,
                    curriculum,
                    false
                );
            })
            .toList();
    }

    /** Nguồn topic cho EXAM_PREP: ngân hàng câu hỏi (question_bank/question_topic) đã PUBLISHED
     * của đúng trường + khối hiện tại của học sinh -- không dùng pool AI-sinh cho ABILITY_IMPROVEMENT.
     * Mỗi topic được vật chất hoá lazy thành 1 dòng practice_topic (nếu chưa có) để toàn bộ pipeline
     * chọn câu hỏi / theo dõi điểm yếu / vector sở thích phía sau chạy nguyên vẹn không cần sửa. */
    private List<RankedTopic> examTopicRanked(UUID studentId) {
        var schoolId = schoolUserRepository.findSchoolIdByUserId(studentId).orElse(null);
        if (schoolId == null) {
            return List.of();
        }
        var gradeId = schoolClassUserRepository.findCurrentSchoolGradeId(studentId).orElse(null);
        if (gradeId == null) {
            return List.of();
        }
        return practiceTopicRepository.findPublishedExamTopics(schoolId, gradeId).stream()
            .map(row -> {
                var topicId = materializeExamTopic(row);
                var interest = 0.5;
                var curriculum = 1.0;
                var base = scoreFor("EXAM_PREP", interest, curriculum);
                return new RankedTopic(
                    topicId,
                    row.getName(),
                    EXAM_TOPIC_INTEREST_DIMENSION,
                    false,
                    base,
                    interest,
                    curriculum,
                    true
                );
            })
            .toList();
    }

    private UUID materializeExamTopic(QuestionTopicInfo row) {
        return practiceTopicRepository.findBySourceQuestionTopicId(row.getId())
            .map(existing -> existing.getId())
            .orElseGet(() -> practiceTopicRepository.save(new PracticeTopic(
                null,
                row.getName(),
                TopicSuggestionService.normalize(row.getName()),
                row.getDescription(),
                "EXAM_QUESTION_BANK",
                EXAM_TOPIC_INTEREST_DIMENSION,
                EXAM_TOPIC_CURRICULUM_GROUP,
                true,
                Instant.now(),
                row.getId(),
                // Chủ đề lấy từ ngân hàng đề của trường: không có ai gán khung thời gian, và
                // đoán từ tên chủ đề thì không có cơ sở. MIXED để thang xoay vòng tự rải thì.
                TensePolicy.AFFORDANCE_MIXED
            )).getId());
    }

    private boolean explorationAllowed(UUID studentId) {
        return practicePaperRepository.countRecentEpsilonPapers(studentId) < 2;
    }

    private String currentGoal(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getGoalType() == null ? "ABILITY_IMPROVEMENT" : profile.getGoalType())
            .orElse("ABILITY_IMPROVEMENT");
    }

    private record RankedTopic(
        UUID id,
        String name,
        String dimension,
        boolean saved,
        double score,
        double interest,
        double curriculum,
        boolean fromSchool) {
    }
}
