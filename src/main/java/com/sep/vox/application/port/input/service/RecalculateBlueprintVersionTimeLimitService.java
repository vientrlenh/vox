package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.service.exam.PaperTimeCalculator;

@Service
public class RecalculateBlueprintVersionTimeLimitService {

    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;

    public RecalculateBlueprintVersionTimeLimitService(
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository) {
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
    }

    @Transactional
    public ExamBlueprintVersion recalculate(UUID blueprintVersionId) {
        var version = examBlueprintVersionRepository.findById(blueprintVersionId).orElseThrow();

        // Chỉ slot FIXED mới có câu hỏi cụ thể để tính; slot RANDOM đóng góp 0 vì chưa biết sẽ bốc
        // câu nào.
        var fixedQuestions = new ArrayList<Question>();
        for (var section : examBlueprintSectionRepository.findByBlueprintVersionId(blueprintVersionId)) {
            for (var slot : examBlueprintSlotRepository.findBySectionId(section.getId())) {
                if (slot.getSlotType() != ExamBlueprintSlotType.FIXED || slot.getFixedQuestionId() == null) {
                    continue;
                }
                questionRepository.findById(slot.getFixedQuestionId()).ifPresent(fixedQuestions::add);
            }
        }

        // Cộng cả thời lượng phát AUDIO/VIDEO: con số này chảy vào requireWithinPlan ở 5 nơi
        // (AttachExamBlueprint, ChangeClassTestBlueprint, Create/UpdateExamBlueprintVersion,
        // CreateExamPaper), tức nó là thước đo ĐỘ DÀI bài thi -- phải đo cả phần media.
        var assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(fixedQuestions.stream().map(Question::getId).distinct().toList()));
        var totalSeconds = PaperTimeCalculator.breakdownOf(fixedQuestions, assetByQuestionId).totalSeconds();

        if (version.getTotalTimeLimitSeconds() == null || version.getTotalTimeLimitSeconds() != totalSeconds) {
            version.setTotalTimeLimitSeconds(totalSeconds);
            return examBlueprintVersionRepository.save(version);
        }
        return version;
    }
}
