package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.question.QuestionAsset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.service.assessment.PaperTimeCalculator;

@Service
@RequiredArgsConstructor
public class RecalculateBlueprintVersionTimeLimitService {

    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;

    @Transactional
    public AssessmentBlueprintVersion recalculate(UUID blueprintVersionId) {
        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(blueprintVersionId).orElseThrow();

        // Chỉ slot FIXED mới có câu hỏi cụ thể để tính; slot RANDOM đóng góp 0 vì chưa biết sẽ bốc
        // câu nào.
        List<Question> fixedQuestions = new ArrayList<Question>();
        for (AssessmentBlueprintSection section : assessmentBlueprintSectionRepository.findByBlueprintVersionId(blueprintVersionId)) {
            for (AssessmentBlueprintSlot slot : assessmentBlueprintSlotRepository.findBySectionId(section.getId())) {
                if (slot.getSlotType() != AssessmentBlueprintSlotType.FIXED || slot.getFixedQuestionId() == null) {
                    continue;
                }
                questionRepository.findById(slot.getFixedQuestionId()).ifPresent(fixedQuestions::add);
            }
        }

        // Includes the AUDIO/VIDEO playback duration: this number flows into requireWithinPlan in 5 places
        // (AttachAssessmentBlueprint, ChangeClassTestBlueprint, Create/UpdateAssessmentBlueprintVersion,
        // CreateAssessmentPaper), i.e. it is the yardstick of the LENGTH of an assessment -- the media part has to be measured too.
        Map<UUID, QuestionAsset> assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(fixedQuestions.stream().map(q -> q.getId()).distinct().toList()));
        int totalSeconds = PaperTimeCalculator.breakdownOf(fixedQuestions, assetByQuestionId).totalSeconds();

        if (version.getTotalTimeLimitSeconds() == null || version.getTotalTimeLimitSeconds() != totalSeconds) {
            version.setTotalTimeLimitSeconds(totalSeconds);
            return assessmentBlueprintVersionRepository.save(version);
        }
        return version;
    }
}
