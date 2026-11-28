package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.question.QuestionAsset;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.ClassTestQuestionCommand;
import com.sep.vox.application.port.input.command.assessment.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.assessment.UpdateClassTestQuestionsCommand;
import com.sep.vox.application.port.input.service.ClassTestDirectEditGuard;
import com.sep.vox.application.port.input.service.ClassTestPaperResolver;
import com.sep.vox.application.port.input.service.AssessmentQuestionSecureLockService;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.service.assessment.PaperTimeCalculator;

@Service
@RequiredArgsConstructor
public class UpdateClassTestQuestionsUseCase implements IUseCase<UpdateClassTestQuestionsCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final ClassTestDirectEditGuard classTestDirectEditGuard;
    private final ClassTestPaperResolver classTestPaperResolver;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentDto execute(UpdateClassTestQuestionsCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        classTestDirectEditGuard.requireDirectlyEditable(assessment, currentUserId);
        requireDistinctQuestionsInSections(input.sections());

        List<Question> candidateQuestions = loadUsableQuestions(input.sections(), assessment, currentUserId);
        requireProjectedDurationWithinPlan(assessment, candidateQuestions);

        AssessmentPaper paper = classTestPaperResolver.resolve(assessment.getId(), input.paperId());
        List<AssessmentPaperSection> existingPaperSections = assessmentPaperSectionRepository.findByPaperId(paper.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();
        applySectionChanges(input, paper, existingPaperSections, assessment, currentUserId);

        Assessment saved = assessmentRepository.save(assessment);
        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentDtoMapper.toDto(saved);
    }

    private void requireDistinctQuestionsInSections(List<ClassTestSectionCommand> sections) {
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("A class test must have at least 1 section");
        }
        Set<UUID> seenQuestionIds = new java.util.HashSet<UUID>();
        for (ClassTestSectionCommand section : sections) {
            if (section.questions() == null || section.questions().isEmpty()) {
                throw new IllegalArgumentException("Every section must have at least 1 question");
            }
            for (ClassTestQuestionCommand questionCommand : section.questions()) {
                if (!seenQuestionIds.add(questionCommand.questionId())) {
                    throw new IllegalArgumentException("A question cannot appear more than once in the same assessment");
                }
            }
        }
    }

    private List<Question> loadUsableQuestions(
            List<ClassTestSectionCommand> sections,
            Assessment assessment,
            UUID currentUserId) {
        List<Question> candidateQuestions = new ArrayList<Question>();
        for (ClassTestSectionCommand section : sections) {
            for (ClassTestQuestionCommand questionCommand : section.questions()) {
                candidateQuestions.add(
                    classTestDirectEditGuard.loadUsableQuestion(assessment, questionCommand.questionId(), currentUserId));
            }
        }
        return candidateQuestions;
    }

    private void requireProjectedDurationWithinPlan(Assessment assessment, List<Question> candidateQuestions) {
        // Projected BEFORE writing so RecalculateAssessmentTimeDurationService cannot be reused. Uses
        // totalSeconds (which includes the AUDIO/VIDEO playback duration) because this is the LENGTH yardstick against the plan.
        Map<UUID, QuestionAsset> assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(candidateQuestions.stream().map(q -> q.getId()).distinct().toList()));
        int candidateDurationSeconds = PaperTimeCalculator
            .breakdownOf(candidateQuestions, assetByQuestionId).totalSeconds();
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            candidateDurationSeconds,
            "Class test paper"
        );
    }

    private void applySectionChanges(
            UpdateClassTestQuestionsCommand input,
            AssessmentPaper paper,
            List<AssessmentPaperSection> existingPaperSections,
            Assessment assessment,
            UUID currentUserId) {
        int commonCount = Math.min(existingPaperSections.size(), input.sections().size());
        List<BigDecimal> sectionWeights = ClassTestSectionWeightPolicy.resolveRequestedWeights(input.sections());

        for (int i = 0; i < commonCount; i++) {
            updateExistingSection(
                existingPaperSections.get(i), input.sections().get(i), sectionWeights.get(i), assessment.getId(), currentUserId);
        }

        for (int i = input.sections().size(); i < existingPaperSections.size(); i++) {
            deleteSection(existingPaperSections.get(i));
        }

        for (int i = existingPaperSections.size(); i < input.sections().size(); i++) {
            createNewSection(paper, input.sections().get(i), sectionWeights.get(i), i + 1, assessment.getId(), currentUserId);
        }
    }

    private void updateExistingSection(
            AssessmentPaperSection existingPaperSection,
            ClassTestSectionCommand sectionCommand,
            BigDecimal sectionWeight,
            UUID assessmentId,
            UUID currentUserId) {
        existingPaperSection.setTitle(sectionCommand.title());
        existingPaperSection.setInstruction(sectionCommand.instruction());
        existingPaperSection.setWeight(sectionWeight);
        assessmentPaperSectionRepository.save(existingPaperSection);

        updateSectionQuestions(existingPaperSection, sectionCommand.questions(), assessmentId, currentUserId);
    }

    private void updateSectionQuestions(
            AssessmentPaperSection paperSection,
            List<ClassTestQuestionCommand> questions,
            UUID assessmentId,
            UUID currentUserId) {
        for (AssessmentPaperItem item : assessmentPaperItemRepository.findBySectionId(paperSection.getId())) {
            assessmentPaperItemRepository.deleteById(item.getId());
        }

        List<BigDecimal> weights = ClassTestSectionWeightPolicy.resolveQuestionWeights(questions);
        for (int i = 0; i < questions.size(); i++) {
            UUID questionId = questions.get(i).questionId();
            assessmentPaperItemRepository.save(AssessmentPaperItem.forQuestion(
                paperSection.getId(), paperSection.getPaperId(), questionId, i + 1, weights.get(i)));
            assessmentQuestionSecureLockService.lockQuestionForAssessment(
                questionId, assessmentId, AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }
    }

    private void deleteSection(AssessmentPaperSection paperSection) {
        for (AssessmentPaperItem item : assessmentPaperItemRepository.findBySectionId(paperSection.getId())) {
            assessmentPaperItemRepository.deleteById(item.getId());
        }
        assessmentPaperSectionRepository.deleteById(paperSection.getId());
    }

    private void createNewSection(
            AssessmentPaper paper,
            ClassTestSectionCommand sectionCommand,
            BigDecimal sectionWeight,
            int order,
            UUID assessmentId,
            UUID currentUserId) {
        AssessmentPaperSection paperSection = assessmentPaperSectionRepository.save(AssessmentPaperSection.forClassTest(
            paper.getId(), order, sectionCommand.title(), sectionCommand.instruction(), sectionWeight));

        List<ClassTestQuestionCommand> questions = sectionCommand.questions();
        List<BigDecimal> weights = ClassTestSectionWeightPolicy.resolveQuestionWeights(questions);
        for (int i = 0; i < questions.size(); i++) {
            UUID questionId = questions.get(i).questionId();
            assessmentPaperItemRepository.save(AssessmentPaperItem.forQuestion(
                paperSection.getId(), paper.getId(), questionId, i + 1, weights.get(i)));
            assessmentQuestionSecureLockService.lockQuestionForAssessment(
                questionId, assessmentId, AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }
    }
}
