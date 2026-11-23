package com.sep.vox.infrastructure.initializer;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sep.vox.domain.model.personalization.InterestDimension;
import com.sep.vox.domain.repository.InterestDimensionRepository;

/**
 * Seeds the catalogue of interest dimensions -- the base table of the whole personalization feature.
 *
 * <p>WHY IT IS NEEDED: previously there was NO path that loaded this table. No migration did an {@code INSERT},
 * no initializer did, and {@code vox-client-web} never built an admin screen for it either (the GraphQL
 * API exists -- see {@code GraphQLInterestDimensionController}). That means every new database
 * had this table EMPTY.
 *
 * <p>Consequence measured on 2026-08-26 on the real environment: a student opened the speaking practice tab, tapped the
 * interest quiz, and nothing happened. No error message, no blank screen -- they just could not get
 * in. The Java log was clean, permissions were granted normally, the agents received no call. Tracing it
 * showed that {@code interest_quiz_item} had 27 questions but {@code interest_dimension} had 0 rows.
 *
 * <p>Failing SILENTLY is the worst part: {@code InterestQuizScorer.normalize} returns {@code Map.of()} when the
 * catalogue is empty instead of reporting an error, so no layer could detect it. It took considerable effort to trace from the
 * UI all the way down to the data table.
 *
 * <p>Idempotent PER CODE rather than by "is the table empty": adding a new dimension to the list
 * below gets it seeded on the next startup, while dimensions whose labels an admin has edited
 * through the UI stay untouched. Checking for an empty table would break both of those.
 */
@Component
@Order(11)
@RequiredArgsConstructor
@Slf4j
public class InterestDimensionInitializer implements ApplicationRunner {

    /**
     * The six dimensions put to the student in the quiz, plus ACADEMIC_ASSESSMENT.
     *
     * <p>The first six codes are NOT chosen arbitrarily: they are exactly the set of codes that the {@code dimensions_json} of
     * the AI-generated quiz questions references (cross-checked against real data). Changing a code here without
     * regenerating the quiz questions makes the quiz lose points on the corresponding dimension, again silently.
     *
     * <p>ACADEMIC_ASSESSMENT is completely different from the rest: {@code quizEligible = false}. It is a system dimension
     * assigned AUTOMATICALLY to topics taken from the question bank ({@code ViewPracticeTopicOffersUseCase}), used to rank
     * topics but not an interest, so students are not asked about it. Without it, topics from the question
     * bank could not be ranked.
     */
    private static final List<InterestDimension> SEED = List.of(
        dimension("TECH_GAMING", "Công nghệ & trò chơi",
            "Thiết bị, phần mềm, game, xu hướng số", true, 1),
        dimension("SPORTS_HEALTH", "Thể thao & sức khoẻ",
            "Vận động, thi đấu, dinh dưỡng, lối sống lành mạnh", true, 2),
        dimension("TRAVEL_PLACES", "Du lịch & vùng đất",
            "Điểm đến, văn hoá vùng miền, trải nghiệm đi lại", true, 3),
        dimension("ENTERTAINMENT_MEDIA", "Giải trí & truyền thông",
            "Phim ảnh, âm nhạc, mạng xã hội, người nổi tiếng", true, 4),
        dimension("PEOPLE_SOCIETY", "Con người & xã hội",
            "Quan hệ, cộng đồng, vấn đề xã hội, giáo dục", true, 5),
        dimension("FUTURE_SCIENCE", "Khoa học & tương lai",
            "Khám phá khoa học, môi trường, xu hướng tương lai", true, 6),
        dimension("ACADEMIC_ASSESSMENT", "Học thuật & luyện đề",
            "Chủ đề lấy từ ngân hàng đề -- hệ thống tự gán, không hỏi học sinh", false, 7)
    );

    private static InterestDimension dimension(
            String code, String label, String description, boolean quizEligible, int displayOrder) {
        Instant now = Instant.now();
        return InterestDimension.builder()
                .code(code)
                .label(label)
                .description(description)
                .active(true)
                .quizEligible(quizEligible)
                .displayOrder(displayOrder)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private final InterestDimensionRepository interestDimensionRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int added = 0;
        for (InterestDimension seed : SEED) {
            if (interestDimensionRepository.findByCode(seed.getCode()).isPresent()) {
                continue;
            }
            interestDimensionRepository.save(seed);
            added++;
        }

        if (added == 0) {
            log.info("Interest dimensions already present. Skip initialize");
            return;
        }
        log.info("Interest dimensions initialized successfully: them {} chieu moi", added);
    }
}
