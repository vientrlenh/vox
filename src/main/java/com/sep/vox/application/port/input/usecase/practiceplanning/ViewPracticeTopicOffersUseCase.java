package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
        var bucket = input.bucket() == null ? "FOR_YOU" : input.bucket();
        var weakCriterion = "BY_WEAKNESS".equals(bucket)
            ? enrichmentService.weakestCriterion(studentId)
            : null;
        var ranked = ("EXAM_PREP".equals(goal)
                ? examTopicRanked(studentId, bucket)
                : classicRanked(studentId, goal, bucket, weakCriterion))
            .stream()
            .filter(topic -> !excluded.contains(topic.id()))
            .sorted(Comparator.comparingDouble(RankedTopic::score).reversed())
            .toList();
        var selected = new ArrayList<>(ranked.subList(0, Math.min(4, ranked.size())));
        if ("FOR_YOU".equals(bucket)) {
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
        }
        Collections.shuffle(selected);
        var minutes = enrichmentService.minutesForStudent(studentId);
        var focusTags = enrichmentService.focusTagsForStudent(studentId);
        var signal = enrichmentService.studentRankSignal(studentId);
        var weakCriterionLabel = weakCriterion == null ? null : enrichmentService.criterionLabel(weakCriterion);
        return selected.stream()
            .map(topic -> {
                var rank = enrichmentService.rankForTopic(studentId, topic.id(), signal);
                var rationale = rationaleFor(topic, weakCriterionLabel);
                return new PracticeTopicOffer(
                    topic.id(),
                    topic.name(),
                    topic.dimension(),
                    topic.saved(),
                    clampPercent(topic.score()),
                    minutes,
                    enrichmentService.levelLabel(rank),
                    rationale,
                    List.of(rationale),
                    focusTags
                );
            })
            .toList();
    }

    private static int clampPercent(double score) {
        return Math.max(0, Math.min(100, (int) Math.round(score * 100)));
    }

    private static String rationaleFor(RankedTopic topic, String weakCriterionLabel) {
        if (weakCriterionLabel != null) {
            return "Còn nhiều câu luyện đúng kỹ năng bạn đang yếu (" + weakCriterionLabel + ")";
        }
        if (topic.curriculum() >= topic.interest() && topic.curriculum() >= topic.bank()) {
            return "Khớp chương trình học của bạn";
        }
        if (topic.bank() >= topic.interest()) {
            return "Bạn chưa luyện nhiều với chủ đề này";
        }
        return "Phù hợp sở thích của bạn";
    }

    private static double scoreFor(String bucket, String goal, double interest, double bank, double curriculum) {
        return switch (bucket) {
            case "BY_GOAL" -> 0.15 * interest + 0.15 * bank + 0.70 * curriculum;
            case "BY_WEAKNESS" -> 0.20 * interest + 0.70 * bank + 0.10 * curriculum;
            default -> "EXAM_PREP".equals(goal)
                ? 0.35 * interest + 0.30 * bank + 0.35 * curriculum
                : 0.60 * interest + 0.25 * bank + 0.15 * curriculum;
        };
    }

    private List<RankedTopic> classicRanked(UUID studentId, String goal, String bucket, String weakCriterion) {
        var rows = "BY_WEAKNESS".equals(bucket) && weakCriterion != null
            ? practiceTopicQueryRepository.findRankedTopicsByWeakness(studentId, goal, weakCriterion, null)
            : practiceTopicQueryRepository.findRankedTopics(studentId, goal);
        return rows.stream()
            .map(row -> {
                var gamma = (double) row.getMentions() / (row.getMentions() + 2);
                var interest = gamma * row.getTopicScore() + (1 - gamma) * row.getDimensionScore();
                var bank = Math.min(1.0, row.getUnseenCount() / 3.0);
                var curriculum = "IN_GDPT2018".equals(row.getCurriculumGroup()) ? 1.0 : 0.3;
                var base = scoreFor(bucket, goal, interest, bank, curriculum);
                return new RankedTopic(
                    row.getId(),
                    row.getName(),
                    row.getInterestDimension(),
                    row.getSavedByMe(),
                    base * (1 - 0.4 * row.getRecency()),
                    interest,
                    bank,
                    curriculum
                );
            })
            .toList();
    }

    /** Nguồn topic cho EXAM_PREP: ngân hàng câu hỏi (question_bank/question_topic) đã PUBLISHED
     * của đúng trường + khối hiện tại của học sinh -- không dùng pool AI-sinh cho ABILITY_IMPROVEMENT.
     * Mỗi topic được vật chất hoá lazy thành 1 dòng practice_topic (nếu chưa có) để toàn bộ pipeline
     * chọn câu hỏi / theo dõi điểm yếu / vector sở thích phía sau chạy nguyên vẹn không cần sửa. */
    private List<RankedTopic> examTopicRanked(UUID studentId, String bucket) {
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
                var bank = 0.5;
                var curriculum = 1.0;
                var base = scoreFor(bucket, "EXAM_PREP", interest, bank, curriculum);
                return new RankedTopic(
                    topicId,
                    row.getName(),
                    EXAM_TOPIC_INTEREST_DIMENSION,
                    false,
                    base,
                    interest,
                    bank,
                    curriculum
                );
            })
            .toList();
    }

    private UUID materializeExamTopic(QuestionTopicInfo row) {
        return practiceTopicRepository.findBySourceQuestionTopicId(row.getId())
            .map(PracticeTopic::id)
            .orElseGet(() -> practiceTopicRepository.save(new PracticeTopic(
                null,
                row.getName(),
                TopicSuggestionService.normalize(row.getName()),
                row.getDescription(),
                "EXAM_QUESTION_BANK",
                EXAM_TOPIC_INTEREST_DIMENSION,
                EXAM_TOPIC_CURRICULUM_GROUP,
                true,
                OffsetDateTime.now(),
                row.getId()
            )).id());
    }

    private boolean explorationAllowed(UUID studentId) {
        return practicePaperRepository.countRecentEpsilonPapers(studentId) < 2;
    }

    private String currentGoal(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.goalType() == null ? "ABILITY_IMPROVEMENT" : profile.goalType())
            .orElse("ABILITY_IMPROVEMENT");
    }

    private record RankedTopic(
        UUID id,
        String name,
        String dimension,
        boolean saved,
        double score,
        double interest,
        double bank,
        double curriculum) {
    }
}
