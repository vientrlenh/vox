package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sep.vox.infrastructure.persistence.entity.rubric.RubricCriterionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.rubric.RubricJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.rubric.RubricResultBandJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.rubric.RubricVersionJpaEntity;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;

/** The atomic rubric statements must tell an omitted field (keep) from an explicit null (clear). */
@RepositoryTest
class RubricAtomicUpdateTests {

    @Autowired
    private SpringDataRubricRepository springDataRubricRepository;

    @Autowired
    private SpringDataRubricVersionRepository springDataRubricVersionRepository;

    @Autowired
    private SpringDataRubricCriterionRepository springDataRubricCriterionRepository;

    @Autowired
    private SpringDataRubricResultBandRepository springDataRubricResultBandRepository;

    @Autowired
    private EntityManager entityManager;

    private static final Instant EFFECTIVE_FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EFFECTIVE_TO = Instant.parse("2026-12-31T00:00:00Z");

    private RubricJpaEntity savedRubric() {
        RubricJpaEntity rubric = RubricJpaEntity.builder().code("R-" + UUID.randomUUID().toString().substring(0, 8))
            .name("Name").description("Description").languageId(UUID.randomUUID()).frameworkId(UUID.randomUUID())
            .ownerType("SYSTEM").build();
        return springDataRubricRepository.saveAndFlush(rubric);
    }

    private RubricJpaEntity reload(UUID id) {
        entityManager.flush();
        entityManager.clear();
        return springDataRubricRepository.findById(id).orElseThrow();
    }

    @Test
    void should_keep_description_when_not_provided() {
        UUID id = savedRubric().getId();

        springDataRubricRepository.updateRubricAtomic(id, "Renamed", null, false);

        RubricJpaEntity reloaded = reload(id);
        assertThat(reloaded.getName()).isEqualTo("Renamed");
        assertThat(reloaded.getDescription()).isEqualTo("Description");
    }

    @Test
    void should_clear_description_when_provided_as_null() {
        UUID id = savedRubric().getId();

        springDataRubricRepository.updateRubricAtomic(id, null, null, true);

        RubricJpaEntity reloaded = reload(id);
        assertThat(reloaded.getName()).isEqualTo("Name");
        assertThat(reloaded.getDescription()).isNull();
    }

    @Test
    void should_replace_description_when_provided_with_a_value() {
        UUID id = savedRubric().getId();

        springDataRubricRepository.updateRubricAtomic(id, null, "New", true);

        assertThat(reload(id).getDescription()).isEqualTo("New");
    }

    private UUID savedVersion() {
        RubricVersionJpaEntity version = RubricVersionJpaEntity.builder().rubricId(UUID.randomUUID())
            .code("V-" + UUID.randomUUID().toString().substring(0, 8)).name("Version").description("Description")
            .version(1).status("DRAFT").effectiveFrom(EFFECTIVE_FROM).effectiveTo(EFFECTIVE_TO)
            .scoringScaleMin(BigDecimal.ZERO).scoringScaleMax(BigDecimal.TEN).totalScoreMethod("SUM").build();
        return springDataRubricVersionRepository.saveAndFlush(version).getId();
    }

    private RubricVersionJpaEntity reloadVersion(UUID id) {
        entityManager.flush();
        entityManager.clear();
        return springDataRubricVersionRepository.findById(id).orElseThrow();
    }

    private void updateVersion(UUID id, String description, boolean descriptionProvided, Instant effectiveTo,
                               boolean effectiveToProvided) {
        springDataRubricVersionRepository.updateRubricVersionAtomic(id, null, null, description, descriptionProvided,
            null, effectiveTo, effectiveToProvided, null, null, null, Instant.now(), "user");
    }

    @Test
    void should_keep_version_description_and_effective_to_when_not_provided() {
        UUID id = savedVersion();

        updateVersion(id, null, false, null, false);

        RubricVersionJpaEntity reloaded = reloadVersion(id);
        assertThat(reloaded.getDescription()).isEqualTo("Description");
        assertThat(reloaded.getEffectiveTo()).isEqualTo(EFFECTIVE_TO);
    }

    @Test
    void should_clear_version_description_and_effective_to_when_provided_as_null() {
        UUID id = savedVersion();

        updateVersion(id, null, true, null, true);

        RubricVersionJpaEntity reloaded = reloadVersion(id);
        assertThat(reloaded.getDescription()).isNull();
        assertThat(reloaded.getEffectiveTo()).isNull();
        assertThat(reloaded.getName()).isEqualTo("Version");
        assertThat(reloaded.getEffectiveFrom()).isEqualTo(EFFECTIVE_FROM);
    }

    @Test
    void should_replace_version_description_and_effective_to_when_provided_with_values() {
        UUID id = savedVersion();
        Instant newEnd = Instant.parse("2027-06-30T00:00:00Z");

        updateVersion(id, "New", true, newEnd, true);

        RubricVersionJpaEntity reloaded = reloadVersion(id);
        assertThat(reloaded.getDescription()).isEqualTo("New");
        assertThat(reloaded.getEffectiveTo()).isEqualTo(newEnd);
    }

    private UUID savedCriterion() {
        RubricCriterionJpaEntity criterion = RubricCriterionJpaEntity.builder().rubricVersionId(UUID.randomUUID())
            .frameworkCriterionId(UUID.randomUUID()).code("C-" + UUID.randomUUID().toString().substring(0, 8))
            .name("Criterion").description("Description").examplesJson("{\"values\":[]}").weight(BigDecimal.ONE)
            .minScore(BigDecimal.ZERO).maxScore(BigDecimal.TEN).order(1).isRequired(true).build();
        return springDataRubricCriterionRepository.saveAndFlush(criterion).getId();
    }

    private RubricCriterionJpaEntity reloadCriterion(UUID id) {
        entityManager.flush();
        entityManager.clear();
        return springDataRubricCriterionRepository.findById(id).orElseThrow();
    }

    private void updateCriterion(UUID id, String description, boolean descriptionProvided, String examplesJson,
                                 boolean examplesJsonProvided) {
        springDataRubricCriterionRepository.updateCriterionAtomic(id, null, null, description, descriptionProvided,
            examplesJson, examplesJsonProvided, null, null, null, null, null, Instant.now(), "user");
    }

    @Test
    void should_keep_criterion_description_and_examples_when_not_provided() {
        UUID id = savedCriterion();

        updateCriterion(id, null, false, null, false);

        RubricCriterionJpaEntity reloaded = reloadCriterion(id);
        assertThat(reloaded.getDescription()).isEqualTo("Description");
        assertThat(reloaded.getExamplesJson()).isEqualTo("{\"values\":[]}");
    }

    @Test
    void should_clear_criterion_description_and_examples_when_provided_as_null() {
        UUID id = savedCriterion();

        updateCriterion(id, null, true, null, true);

        RubricCriterionJpaEntity reloaded = reloadCriterion(id);
        assertThat(reloaded.getDescription()).isNull();
        assertThat(reloaded.getExamplesJson()).isNull();
        assertThat(reloaded.getName()).isEqualTo("Criterion");
        assertThat(reloaded.getWeight()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void should_replace_criterion_description_and_examples_when_provided_with_values() {
        UUID id = savedCriterion();

        updateCriterion(id, "New", true, "{\"values\":[1]}", true);

        RubricCriterionJpaEntity reloaded = reloadCriterion(id);
        assertThat(reloaded.getDescription()).isEqualTo("New");
        assertThat(reloaded.getExamplesJson()).isEqualTo("{\"values\":[1]}");
    }

    private UUID savedResultBand() {
        RubricResultBandJpaEntity band = RubricResultBandJpaEntity.builder().rubricVersionId(UUID.randomUUID())
            .code("B-" + UUID.randomUUID().toString().substring(0, 8)).name("Band").description("Description")
            .scoreMin(BigDecimal.ZERO).scoreMax(BigDecimal.TEN).order(1).build();
        return springDataRubricResultBandRepository.saveAndFlush(band).getId();
    }

    private RubricResultBandJpaEntity reloadResultBand(UUID id) {
        entityManager.flush();
        entityManager.clear();
        return springDataRubricResultBandRepository.findById(id).orElseThrow();
    }

    private void updateResultBand(UUID id, String description, boolean descriptionProvided) {
        springDataRubricResultBandRepository.updateResultBandAtomic(id, null, null, description, descriptionProvided,
            null, null, null, Instant.now(), "user");
    }

    @Test
    void should_keep_result_band_description_when_not_provided() {
        UUID id = savedResultBand();

        updateResultBand(id, null, false);

        assertThat(reloadResultBand(id).getDescription()).isEqualTo("Description");
    }

    @Test
    void should_clear_result_band_description_when_provided_as_null() {
        UUID id = savedResultBand();

        updateResultBand(id, null, true);

        RubricResultBandJpaEntity reloaded = reloadResultBand(id);
        assertThat(reloaded.getDescription()).isNull();
        assertThat(reloaded.getName()).isEqualTo("Band");
    }

    @Test
    void should_replace_result_band_description_when_provided_with_a_value() {
        UUID id = savedResultBand();

        updateResultBand(id, "New", true);

        assertThat(reloadResultBand(id).getDescription()).isEqualTo("New");
    }
}
