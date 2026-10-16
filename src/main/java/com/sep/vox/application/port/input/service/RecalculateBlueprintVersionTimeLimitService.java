package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.service.assessment.PaperTimeCalculator;

@Service
public class RecalculateBlueprintVersionTimeLimitService {

    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;

    public RecalculateBlueprintVersionTimeLimitService(
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository,
            AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository) {
        this.assessmentBlueprintVersionRepository = assessmentBlueprintVersionRepository;
        this.assessmentBlueprintSectionRepository = assessmentBlueprintSectionRepository;
        this.assessmentBlueprintSlotRepository = assessmentBlueprintSlotRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
    }

    @Transactional
    public AssessmentBlueprintVersion recalculate(UUID blueprintVersionId) {
        var version = assessmentBlueprintVersionRepository.findById(blueprintVersionId).orElseThrow();

        // Chỉ slot FIXED mới có câu hỏi cụ thể để tính; slot RANDOM đóng góp 0 vì chưa biết sẽ bốc
        // câu nào.
        var fixedQuestions = new ArrayList<Question>();
        for (var section : assessmentBlueprintSectionRepository.findByBlueprintVersionId(blueprintVersionId)) {
            for (var slot : assessmentBlueprintSlotRepository.findBySectionId(section.getId())) {
                if (slot.getSlotType() != AssessmentBlueprintSlotType.FIXED || slot.getFixedQuestionId() == null) {
                    continue;
                }
                questionRepository.findById(slot.getFixedQuestionId()).ifPresent(fixedQuestions::add);
            }
        }

        // Cộng cả thời lượng phát AUDIO/VIDEO: con số này chảy vào requireWithinPlan ở 5 nơi
        // (AttachAssessmentBlueprint, ChangeClassTestBlueprint, Create/UpdateAssessmentBlueprintVersion,
        // CreateAssessmentPaper), tức nó là thước đo ĐỘ DÀI bài thi -- phải đo cả phần media.
        var assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(fixedQuestions.stream().map(q -> q.getId()).distinct().toList()));
        var totalSeconds = PaperTimeCalculator.breakdownOf(fixedQuestions, assetByQuestionId).totalSeconds();

        if (version.getTotalTimeLimitSeconds() == null || version.getTotalTimeLimitSeconds() != totalSeconds) {
            version.setTotalTimeLimitSeconds(totalSeconds);
            return assessmentBlueprintVersionRepository.save(version);
        }
        return version;
    }
}
