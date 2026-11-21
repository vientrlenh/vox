package com.sep.vox.application.port.input.usecase.practiceplanning;

import com.sep.vox.application.query.dto.RankedTopicInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.PracticePaperRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewPracticeTopicOffersUseCase implements IUseCase<ViewPracticeTopicOffersQuery, List<PracticeTopicOffer>> {

    private static final String ASSESSMENT_TOPIC_INTEREST_DIMENSION = "ACADEMIC_ASSESSMENT";
    private static final String ASSESSMENT_TOPIC_CURRICULUM_GROUP = "ASSESSMENT_BANK";

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
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        if (!hasCompletedInterestQuiz(studentId)) {
            return List.of();
        }
        Set<UUID> excluded = new HashSet<>(
            input.excludeTopicIds() == null ? List.<UUID>of() : input.excludeTopicIds()
        );
        String goal = currentGoal(studentId);
        // `bucket` không còn phân nhánh gì: enum chỉ còn FOR_YOU (xem practice-planning.graphqls).
        List<RankedTopic> ranked = ("ASSESSMENT_PREP".equals(goal)
                ? assessmentTopicRanked(studentId)
                : classicRanked(studentId, goal))
            .stream()
            .filter(topic -> !excluded.contains(topic.id()))
            .sorted(Comparator.comparingDouble((RankedTopic topic) -> topic.score()).reversed())
            .toList();
        List<RankedTopic> selected = new ArrayList<>(ranked.subList(0, Math.min(4, ranked.size())));
        double epsilon = input.round() >= 2 ? 0.30 : 0.10;
        if (selected.size() == 4
                && ranked.size() > 10
                && explorationAllowed(studentId)
                && ThreadLocalRandom.current().nextDouble() < epsilon) {
            List<RankedTopic> exploratory = ranked.subList(10, ranked.size());
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
        int minutes = enrichmentService.minutesForStudent(studentId);
        return selected.stream()
            .map(topic -> {
                String rationale = rationaleFor(topic);
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
        String goal = currentGoal(studentId);
        if ("ASSESSMENT_PREP".equals(goal)) {
            // The ASSESSMENT_PREP path does not run epsilon-greedy (only the FOR_YOU bucket of the AI-generated pool has it).
            return false;
        }
        List<RankedTopic> ranked = classicRanked(studentId, goal).stream()
            .sorted(Comparator.comparingDouble((RankedTopic topic) -> topic.score()).reversed())
            .toList();
        int position = -1;
        for (int index = 0; index < ranked.size(); index++) {
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
        return "ASSESSMENT_PREP".equals(goal)
            ? 0.50 * interest + 0.50 * curriculum
            : 0.80 * interest + 0.20 * curriculum;
    }

    private List<RankedTopic> classicRanked(UUID studentId, String goal) {
        List<RankedTopicInfo> rows = practiceTopicQueryRepository.findRankedTopics(studentId, goal);
        return rows.stream()
            .map(row -> {
                double gamma = (double) row.getMentions() / (row.getMentions() + 2);
                double interest = gamma * row.getTopicScore() + (1 - gamma) * row.getDimensionScore();
                double curriculum = "IN_GDPT2018".equals(row.getCurriculumGroup()) ? 1.0 : 0.3;
                double base = scoreFor(goal, interest, curriculum);
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

    /**
     * Topic source for ASSESSMENT_PREP: the PUBLISHED question bank (question_bank/question_topic)
     * of exactly the student's current school + grade -- the AI-generated pool for ABILITY_IMPROVEMENT is not used.
     * Each topic is lazily materialized into 1 practice_topics row (if it does not exist yet) so that the whole downstream pipeline of
     * question selection / weakness tracking / interest vector runs intact without changes.
     */
    private List<RankedTopic> assessmentTopicRanked(UUID studentId) {
        UUID schoolId = schoolUserRepository.findSchoolIdByUserId(studentId).orElse(null);
        if (schoolId == null) {
            return List.of();
        }
        UUID gradeId = schoolClassUserRepository.findCurrentSchoolGradeId(studentId).orElse(null);
        if (gradeId == null) {
            return List.of();
        }
        return practiceTopicQueryRepository.findPublishedAssessmentTopics(schoolId, gradeId).stream()
            .map(row -> {
                UUID topicId = materializeAssessmentTopic(row);
                double interest = 0.5;
                double curriculum = 1.0;
                double base = scoreFor("ASSESSMENT_PREP", interest, curriculum);
                return new RankedTopic(
                    topicId,
                    row.getName(),
                    ASSESSMENT_TOPIC_INTEREST_DIMENSION,
                    false,
                    base,
                    interest,
                    curriculum,
                    true
                );
            })
            .toList();
    }

    private UUID materializeAssessmentTopic(QuestionTopicInfo row) {
        return practiceTopicRepository.findBySourceQuestionTopicId(row.getId())
            .map(existing -> existing.getId())
            .orElseGet(() -> practiceTopicRepository.save(PracticeTopic.builder()
                .name(row.getName())
                .normalizedName(TopicSuggestionService.normalize(row.getName()))
                .description(row.getDescription())
                .source("ASSESSMENT_QUESTION_BANK")
                .interestDimension(ASSESSMENT_TOPIC_INTEREST_DIMENSION)
                .curriculumGroup(ASSESSMENT_TOPIC_CURRICULUM_GROUP)
                .active(true)
                .createdAt(Instant.now())
                .sourceQuestionTopicId(row.getId())
                // The topic comes from the school's question bank: nobody assigned a tense, and guessing from the
                // topic name has no basis. MIXED lets the rotating scale spread the tenses itself.
                .temporalAffordance(TensePolicy.AFFORDANCE_MIXED)
                .build()).getId());
    }

    private boolean explorationAllowed(UUID studentId) {
        return practicePaperRepository.countRecentEpsilonPapers(studentId) < 2;
    }

    private String currentGoal(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getGoalType() == null ? "ABILITY_IMPROVEMENT" : profile.getGoalType())
            .orElse("ABILITY_IMPROVEMENT");
    }

    /**
     * Chưa nộp quiz sở thích thì KHÔNG chào chủ đề nào.
     *
     * <p>Cổng này phải nằm ở server chứ không phải ở app. Cổng bên Flutter chỉ gác tab "Luyện
     * tập", trong khi màn Trang chủ -- tab hiện ra ngay sau khi đăng nhập -- cũng gọi
     * {@code practiceTopicOffers} để dựng thẻ "Chủ đề hôm nay" (xem
     * {@code PersonalizeRepository.getDashboard}). Nên chỉ cần đăng nhập là lô chào đã chạy,
     * không cần chạm vào tab nào.
     *
     * <p>Không chặn thì {@code dimension_interest_scores} rỗng và câu xếp hạng lấp bằng
     * {@code COALESCE(..., 0.5)}: mọi chủ đề trong chương trình ra đúng cùng một điểm, client
     * vẫn in "% khớp" từ con số đó. Danh sách trông như đã cá nhân hoá trong khi chưa có một
     * tín hiệu nào -- sai lặng lẽ, không lỗi.
     *
     * <p>Hệ quả nữa, quan trọng hơn: lô chào rỗng khiến controller kích hoạt sinh bù, mà lượt
     * sinh đó gửi bản đồ sở thích RỖNG sang LLM. Đo được trên production 2026-08-15: 111 học
     * sinh, 0 hồ sơ, 0 phiên luyện, nhưng kho đã có 6 chủ đề chung chung sinh lúc 10:25 --
     * và kho là của chung nên cả 111 em đều nhận chúng. Xem thêm cổng cùng cặp ở
     * {@code TopicOfferBackfillService.backfillAsync}.
     */
    private boolean hasCompletedInterestQuiz(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getQuizCompletedAt() != null)
            .orElse(false);
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
