package com.sep.vox.application.common;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.RubricCriterionBandRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.valueobject.scoringruleaction.CapCriterionScoreParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CapFrameworkResultBandParams;
import com.sep.vox.domain.valueobject.scoringruleaction.CriterionScoreDeltaParams;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringruleaction.SetFrameworkResultBandParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.CriterionBandThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.CriterionScoreThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;

// Kiểm tra criterionCode/bandCode (điền dạng code, không phải UUID) trong conditionParams/actionParams
// của ScoringRule có thực sự tồn tại trong Rubric Version của Assessment Policy hay không, và
// frameworkResultBandId (nếu có) có thực sự tồn tại và thuộc đúng Framework Version của Policy hay không.
// Không đặt logic này trong ScoringRuleParamsMapper vì mapper chỉ convert JSON thô, không có
// dependency vào repository.
@Component
public class ScoringRuleCriterionValidator {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricCriterionBandRepository rubricCriterionBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;

    public ScoringRuleCriterionValidator(
            RubricCriterionRepository rubricCriterionRepository,
            RubricCriterionBandRepository rubricCriterionBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository) {
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricCriterionBandRepository = rubricCriterionBandRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
    }

    public void validate(UUID rubricVersionId, UUID frameworkVersionId,
            ScoringRuleConditionParams conditionParams, ScoringRuleActionParams actionParams) {
        switch (conditionParams) {
            case CriterionScoreThresholdParams p -> requireCriterion(rubricVersionId, p.criterionCode());
            case CriterionBandThresholdParams p -> {
                RubricCriterion criterion = requireCriterion(rubricVersionId, p.criterionCode());
                requireCriterionBand(criterion, p.bandCode());
            }
            default -> {
            }
        }

        switch (actionParams) {
            case CapCriterionScoreParams p -> requireCriterion(rubricVersionId, p.criterionCode());
            case CriterionScoreDeltaParams p -> requireCriterion(rubricVersionId, p.criterionCode());
            case SetFrameworkResultBandParams p -> requireFrameworkResultBand(frameworkVersionId, p.frameworkResultBandId());
            case CapFrameworkResultBandParams p -> requireFrameworkResultBand(frameworkVersionId, p.maxResultBandId());
            default -> {
            }
        }
    }

    private RubricCriterion requireCriterion(UUID rubricVersionId, String criterionCode) {
        return rubricCriterionRepository.findByRubricVersionIdAndCode(rubricVersionId, criterionCode)
                .orElseThrow(() -> new NotFoundException(
                        "Mã tiêu chí (criterionCode) '" + criterionCode + "' không tồn tại trong Rubric Version của Assessment Policy này."));
    }

    private void requireCriterionBand(RubricCriterion criterion, String bandCode) {
        rubricCriterionBandRepository.findByCriterionIdAndCode(criterion.getId(), bandCode)
                .orElseThrow(() -> new NotFoundException(
                        "Mã mức độ (bandCode) '" + bandCode + "' không tồn tại trong tiêu chí '" + criterion.getCode() + "'."));
    }

    private void requireFrameworkResultBand(UUID frameworkVersionId, UUID frameworkResultBandId) {
        FrameworkResultBand band = frameworkResultBandRepository.findById(frameworkResultBandId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy Framework Result Band với ID '" + frameworkResultBandId + "'."));
        if (!band.getFrameworkVersionId().equals(frameworkVersionId)) {
            throw new NotFoundException(
                    "Framework Result Band '" + frameworkResultBandId + "' không thuộc Framework Version của Assessment Policy này.");
        }
    }
}