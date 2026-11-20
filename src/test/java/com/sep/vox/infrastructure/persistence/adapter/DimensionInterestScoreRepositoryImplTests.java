package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.repository.DimensionInterestScoreRepository;

/**
 * Học sinh làm lại quiz sở thích -- mọi lần thứ hai trở đi -- gọi {@code replaceScores} với cùng
 * {@code learner_profile_id} đã có dòng cũ. Chạy trên DB thật vì lỗi này là lỗi THỨ TỰ FLUSH của
 * Hibernate: {@code deleteByLearnerProfileId} (không {@code @Modifying}) chỉ xếp hàng một
 * {@code EntityDeleteAction}, và Hibernate luôn thực thi insert TRƯỚC delete trong hàng đợi flush
 * bất kể thứ tự gọi trong code -- unit test với repository giả lập không bao giờ thấy được điều
 * này, vì mock không có hàng đợi flush nào cả.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class DimensionInterestScoreRepositoryImplTests extends ContainerTestConfig {

    @Autowired
    private DimensionInterestScoreRepository repository;

    /**
     * Tái hiện đúng lỗi thật: {@code idx_dimension_interest_profile_dimension} nổ trên INSERT
     * ngay trong lần replaceScores THỨ HAI, vì dòng cũ (cùng dimension) chưa kịp bị xoá lúc
     * saveAll chạy.
     */
    @Test
    void should_replace_scores_again_without_violating_the_unique_index() {
        var profileId = UUID.randomUUID();

        repository.replaceScores(profileId, Map.of(
            "TECH_GAMING", 0.6202,
            "TRAVEL_PLACES", 0.1667
        ));

        assertThatCode(() -> repository.replaceScores(profileId, Map.of(
            "TECH_GAMING", 0.7000,
            "SPORTS_HEALTH", 0.2000
        ))).doesNotThrowAnyException();

        var current = repository.findByLearnerProfile(profileId);
        assertThat(current).containsEntry("TECH_GAMING", 0.7000);
        assertThat(current).containsEntry("SPORTS_HEALTH", 0.2000);
        // TRAVEL_PLACES thuộc lần đầu, không có trong lần thứ hai -- replaceScores nghĩa là THAY
        // HẲN, không phải cộng dồn.
        assertThat(current).doesNotContainKey("TRAVEL_PLACES");
    }

    /** Ba lần liên tiếp, đúng như học sinh bấm làm lại quiz nhiều lần trong một buổi. */
    @Test
    void should_survive_three_consecutive_replacements() {
        var profileId = UUID.randomUUID();

        for (var round = 1; round <= 3; round++) {
            var roundScore = 0.1 * round;
            assertThatCode(() -> repository.replaceScores(profileId, Map.of("TECH_GAMING", roundScore)))
                .doesNotThrowAnyException();
        }

        assertThat(repository.findByLearnerProfile(profileId)).containsEntry("TECH_GAMING", 0.3);
    }
}
