package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

/**
 * Copies a system rubric version into a rubric owned by a school.
 *
 * <p>The copy is deep across four levels: {@link Rubric} -> {@link RubricVersion} -> {@link RubricCriterion} +
 * {@link RubricResultBand}. From the moment it is created the copy is an independent asset of the school:
 * editing it never touches the template, and later changes to the template do not flow into it.
 */
@Service
public class RubricCloneService {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;

    public RubricCloneService(
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricResultBandRepository rubricResultBandRepository) {
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
    }

    /**
     * @param targetMethod the scoring method the school chose for the copy, which may differ from the template's
     *                     -- see {@link #resolveWeight}.
     * @return the version that was just created, always in DRAFT status.
     */
    @Transactional
    public RubricVersion cloneToSchoolAsDraft(
            Rubric sourceRubric,
            RubricVersion sourceVersion,
            UUID schoolId,
            String code,
            String name,
            String description,
            RubricTotalScoreMethod targetMethod,
            UUID currentUserId) {

        Instant now = Instant.now();

        // languageId and frameworkId follow the template and must NOT be chosen again: the criteria
        // below point to the frameworkCriterionId of the source framework, so changing the framework
        // would leave all of those references dangling.
        Rubric clonedRubric = rubricRepository.save(Rubric.builder()
                .languageId(sourceRubric.getLanguageId())
                .frameworkId(sourceRubric.getFrameworkId())
                .code(code)
                .name(name)
                .description(description)
                .ownerType(RubricOwnerType.SCHOOL)
                .schoolId(schoolId)
                .build());

        RubricVersion clonedVersion = RubricVersion.builder()
                .rubricId(clonedRubric.getId())
                // A new rubric always restarts versioning at 1. The version number is the TIME axis within
                // one rubric, not something inherited from the template -- a clone of "template v3" is
                // still the school's first version.
                .version(1)
                .code(code)
                .name(name)
                .description(description)
                // DRAFT is mandatory, not a convention: CreateSchoolAssessmentPolicyUseCase rejects
                // attaching a policy to an already PUBLISHED version, so a clone created directly as
                // PUBLISHED would be one the school can never attach any policy to -- i.e. useless.
                .status(RubricStatus.DRAFT)
                .effectiveFrom(now)
                .effectiveTo(null)
                .scoringScaleMin(sourceVersion.getScoringScaleMin())
                .scoringScaleMax(sourceVersion.getScoringScaleMax())
                .totalScoreMethod(targetMethod)
                .build();
        clonedVersion.setSourceRubricVersionId(sourceVersion.getId());
        RubricVersion savedVersion = rubricVersionRepository.save(clonedVersion);

        cloneCriteria(sourceVersion.getId(), savedVersion.getId(), targetMethod, currentUserId, now);
        cloneResultBands(sourceVersion.getId(), savedVersion.getId(), currentUserId, now);

        return savedVersion;
    }

    private void cloneCriteria(
            UUID sourceVersionId, UUID targetVersionId, RubricTotalScoreMethod targetMethod,
            UUID currentUserId, Instant now) {
        List<RubricCriterion> sourceCriteria = rubricCriterionRepository.findByRubricVersionId(sourceVersionId);
        if (sourceCriteria.isEmpty()) {
            return;
        }
        rubricCriterionRepository.saveAll(sourceCriteria.stream()
            .map(source -> RubricCriterion.builder()
                    .rubricVersionId(targetVersionId)
                    .frameworkCriterionId(source.getFrameworkCriterionId())
                    .code(source.getCode())
                    .name(source.getName())
                    .description(source.getDescription())
                    .examples(source.getExamples())
                    .weight(resolveWeight(source.getWeight(), targetMethod))
                    .minScore(source.getMinScore())
                    .maxScore(source.getMaxScore())
                    .order(source.getOrder())
                    .required(source.isRequired())
                    .build())
            .toList());
    }

    /**
     * Weight for the copy, according to the scoring method the school chose.
     *
     * <p>The two methods use two different weight conventions, and only one direction can be converted:
     *
     * <ul>
     *   <li><b>Distributed (SUM)</b>: the weights add up to 100%, and every number carries an intent about the
     *       relative importance of the criteria.
     *   <li><b>Average (WEIGHTED_AVERAGE)</b>: every criterion is balanced at 100%.
     * </ul>
     *
     * <p>So Average can be derived from Distributed by flattening every weight to 1 -- that is what choosing
     * Average means, not an unintended loss. The opposite direction is impossible: from a state where all
     * criteria are equal there is no way to rebuild the proportions the template defined. That is why the
     * template should be authored as Distributed -- it carries more information.
     *
     * <p>The result always satisfies {@link com.sep.vox.domain.service.rubric.RubricScoringConsistencyValidator},
     * so the copy can be published as soon as the school has attached a policy to it.
     */
    private BigDecimal resolveWeight(BigDecimal sourceWeight, RubricTotalScoreMethod targetMethod) {
        if (targetMethod == RubricTotalScoreMethod.WEIGHTED_AVERAGE) {
            return BigDecimal.ONE;
        }
        return sourceWeight;
    }

    private void cloneResultBands(UUID sourceVersionId, UUID targetVersionId, UUID currentUserId, Instant now) {
        List<RubricResultBand> sourceBands = rubricResultBandRepository.findByRubricVersionId(sourceVersionId);
        if (sourceBands.isEmpty()) {
            return;
        }
        rubricResultBandRepository.saveAll(sourceBands.stream()
            .map(source -> RubricResultBand.builder()
                    .rubricVersionId(targetVersionId)
                    .code(source.getCode())
                    .name(source.getName())
                    .description(source.getDescription())
                    .scoreMin(source.getScoreMin())
                    .scoreMax(source.getScoreMax())
                    .order(source.getOrder())
                    .build())
            .toList());
    }
}
