package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class RecalculateBlueprintVersionTimeLimitService {

    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final QuestionRepository questionRepository;

    public RecalculateBlueprintVersionTimeLimitService(
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            QuestionRepository questionRepository) {
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public ExamBlueprintVersion recalculate(UUID blueprintVersionId) {
        var version = examBlueprintVersionRepository.findById(blueprintVersionId).orElseThrow();
        var totalSeconds = 0;

        for (var section : examBlueprintSectionRepository.findByBlueprintVersionId(blueprintVersionId)) {
            for (var slot : examBlueprintSlotRepository.findBySectionId(section.getId())) {
                if (slot.getSlotType() != ExamBlueprintSlotType.FIXED || slot.getFixedQuestionId() == null) {
                    continue;
                }
                var question = questionRepository.findById(slot.getFixedQuestionId()).orElse(null);
                if (question != null) {
                    totalSeconds += question.getPreparationTimeSeconds() + question.getMaxResponseSeconds();
                }
            }
        }

        if (version.getTotalTimeLimitSeconds() == null || version.getTotalTimeLimitSeconds() != totalSeconds) {
            version.setTotalTimeLimitSeconds(totalSeconds);
            return examBlueprintVersionRepository.save(version);
        }
        return version;
    }
}
