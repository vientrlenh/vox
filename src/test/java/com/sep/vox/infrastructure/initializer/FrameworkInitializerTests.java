package com.sep.vox.infrastructure.initializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionCode;
import com.sep.vox.support.IntegrationTest;

/**
 * {@code ApplicationRunner} KHÔNG được gọi trong {@code @SpringBootTest} -- đó là lý do một
 * bản {@code FrameworkInitializer} chỉ có {@code throw new UnsupportedOperationException}
 * vẫn để toàn bộ suite xanh. Nghĩa là không có lưới nào khác đỡ cho lớp này ngoài chính nó.
 *
 * <p>Vì vậy ở đây gọi thẳng {@code run(null)} trên bean thật, với DB thật: khoá ngoại, ràng
 * buộc unique và phần serialize signals sang JSON chỉ lộ lỗi khi có lượt ghi thật sự.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class FrameworkInitializerTests {

    private static final String FRAMEWORK_CODE = "KNLNNVN";
    private static final String FRAMEWORK_VERSION_CODE = "KNLNNVN_V1";
    private static final int EXPECTED_RESULT_BANDS = 6;

    @Autowired
    private FrameworkInitializer frameworkInitializer;

    @Autowired
    private FrameworkRepository frameworkRepository;

    @Autowired
    private FrameworkVersionRepository frameworkVersionRepository;

    @Autowired
    private FrameworkCriterionRepository frameworkCriterionRepository;

    @Autowired
    private FrameworkResultBandRepository frameworkResultBandRepository;

    @Autowired
    private FrameworkCriterionBandRepository frameworkCriterionBandRepository;

    /**
     * Kết quả quan trọng nhất: sau khi chạy, luyện tập phải tra ra được một bản đang hiệu
     * lực. Thiếu bất kỳ điều kiện nào trong findActiveVersionId (PUBLISHED, framework active,
     * effectiveFrom đã tới) thì mọi phiên luyện tập đều không vào được.
     */
    @Test
    void should_seed_a_framework_version_that_practice_can_actually_resolve() throws Exception {
        frameworkInitializer.run(null);

        assertThat(frameworkVersionRepository.findActiveVersionId(null))
            .as("luyện tập phải tra ra được bản đang hiệu lực")
            .isPresent();
    }

    @Test
    void should_seed_published_framework_and_version() throws Exception {
        frameworkInitializer.run(null);

        Optional<Framework> framework = frameworkRepository.findByCode(FRAMEWORK_CODE);
        assertThat(framework).isPresent();
        assertThat(framework.get().isActive()).isTrue();

        Optional<FrameworkVersion> version = frameworkVersionRepository.findByCode(FRAMEWORK_VERSION_CODE);
        assertThat(version).isPresent();
        assertThat(version.get().getStatus()).isEqualTo(FrameworkVersionStatus.PUBLISHED);
        assertThat(version.get().getEffectiveFrom()).isNotNull();
        assertThat(version.get().getEffectiveTo()).isNull();
    }

    /**
     * UpdateFrameworkVersionStatusUseCase compares the set of codes with equals() when publishing.
     * The seed writes straight through the repository and so bypasses that check -- this test
     * replaces it.
     */
    @Test
    void seeded_criteria_must_match_the_allowed_code_set_exactly() throws Exception {
        frameworkInitializer.run(null);

        UUID versionId = frameworkVersionRepository.findByCode(FRAMEWORK_VERSION_CODE).orElseThrow().getId();
        List<FrameworkCriterion> criteria = frameworkCriterionRepository.findByFrameworkVersionId(versionId);

        assertThat(criteria).extracting(criterion -> criterion.getCode())
            .containsExactlyInAnyOrderElementsOf(FrameworkCriterionCode.ALLOWED_CODES);
    }

    /**
     * The remaining condition for this version to be valid in the PUBLISHED state: every criterion
     * has all 6 levels, and every level has at least one positive indicator and one negative
     * indicator.
     */
    @Test
    void every_criterion_must_have_all_bands_with_both_signal_directions() throws Exception {
        frameworkInitializer.run(null);

        UUID versionId = frameworkVersionRepository.findByCode(FRAMEWORK_VERSION_CODE).orElseThrow().getId();
        List<FrameworkCriterion> criteria = frameworkCriterionRepository.findByFrameworkVersionId(versionId);
        List<FrameworkResultBand> resultBands = frameworkResultBandRepository.findByFrameworkVersionId(versionId);
        assertThat(resultBands).hasSize(EXPECTED_RESULT_BANDS);

        List<UUID> criterionIds = criteria.stream().map(criterion -> criterion.getId()).toList();
        List<FrameworkCriterionBand> bands = frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds);

        assertThat(bands).hasSize(criteria.size() * EXPECTED_RESULT_BANDS);
        assertThat(bands).allSatisfy(band -> {
            assertThat(band.getDescriptor()).isNotBlank();
            assertThat(band.getPositiveSignals().values()).isNotEmpty();
            assertThat(band.getNegativeSignals().values()).isNotEmpty();
        });
    }

    /** Running again at a later startup must not duplicate the data. */
    @Test
    void should_be_idempotent_across_restarts() throws Exception {
        frameworkInitializer.run(null);
        frameworkInitializer.run(null);

        UUID versionId = frameworkVersionRepository.findByCode(FRAMEWORK_VERSION_CODE).orElseThrow().getId();
        assertThat(frameworkCriterionRepository.findByFrameworkVersionId(versionId))
            .hasSize(FrameworkCriterionCode.ALLOWED_CODES.size());
        assertThat(frameworkResultBandRepository.findByFrameworkVersionId(versionId))
            .hasSize(EXPECTED_RESULT_BANDS);
    }
}
