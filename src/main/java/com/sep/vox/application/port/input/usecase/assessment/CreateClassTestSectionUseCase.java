package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.application.port.input.command.assessment.ClassTestQuestionCommand;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.CreateClassTestSectionCommand;
import com.sep.vox.application.port.input.service.AssessmentQuestionSecureLockService;
import com.sep.vox.application.port.input.service.ClassTestDirectEditGuard;
import com.sep.vox.application.port.input.service.ClassTestPaperResolver;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

@Service
@RequiredArgsConstructor
public class CreateClassTestSectionUseCase implements IUseCase<CreateClassTestSectionCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final ClassTestDirectEditGuard classTestDirectEditGuard;
    private final ClassTestPaperResolver classTestPaperResolver;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentDto execute(CreateClassTestSectionCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        classTestDirectEditGuard.requireDirectlyEditable(assessment, currentUserId);
        if (input.questions() == null || input.questions().isEmpty()) {
            throw new IllegalArgumentException("A section must have at least 1 question");
        }
        requireQuestionsUsable(input.questions(), assessment, currentUserId);

        AssessmentPaper paper = classTestPaperResolver.resolve(assessment.getId(), input.paperId());
        int order = assessmentPaperSectionRepository.findByPaperId(paper.getId()).size() + 1;

        AssessmentPaperSection paperSection = assessmentPaperSectionRepository.save(AssessmentPaperSection.forClassTest(
            paper.getId(), order, input.title(), input.instruction(), input.weight()));

        saveAndLockQuestions(input.questions(), paperSection, paper, assessment, currentUserId);
        rebalanceOrValidateSectionWeights(input.weight(), paper.getId());

        Assessment saved = assessmentRepository.save(assessment);
        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentDtoMapper.toDto(saved);
    }

    private void rebalanceOrValidateSectionWeights(BigDecimal requestedWeight, UUID paperId) {
        if (requestedWeight == null) {
            rebalanceSectionWeights(paperId);
        } else {
            List<AssessmentPaperSection> sections = assessmentPaperSectionRepository.findByPaperId(paperId).stream()
                .sorted(Comparator.comparingInt(section -> section.getOrder()))
                .toList();
            ClassTestSectionWeightPolicy.validateStoredWeights(sections, "The total section weight must be 1.00");
        }
    }

    private void requireQuestionsUsable(List<ClassTestQuestionCommand> questions, Assessment assessment, UUID currentUserId) {
        for (ClassTestQuestionCommand questionCommand : questions) {
            classTestDirectEditGuard.loadUsableQuestion(assessment, questionCommand.questionId(), currentUserId);
        }
    }

    private void saveAndLockQuestions(
            List<ClassTestQuestionCommand> questions,
            AssessmentPaperSection paperSection,
            AssessmentPaper paper,
            Assessment assessment,
            UUID currentUserId) {
        List<BigDecimal> weights = ClassTestSectionWeightPolicy.resolveQuestionWeights(questions);
        for (int i = 0; i < questions.size(); i++) {
            UUID questionId = questions.get(i).questionId();
            assessmentPaperItemRepository.save(AssessmentPaperItem.forQuestion(
                paperSection.getId(), paper.getId(), questionId, i + 1, weights.get(i)));
            assessmentQuestionSecureLockService.lockQuestionForAssessment(
                questionId, assessment.getId(), AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }
    }

    private List<BigDecimal> distributeEqualWeights(int count) {
        List<BigDecimal> weights = new ArrayList<BigDecimal>();
        BigDecimal perItem = BigDecimal.ONE.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal runningSum = BigDecimal.ZERO;
        for (int i = 0; i < count - 1; i++) {
            weights.add(perItem);
            runningSum = runningSum.add(perItem);
        }
        weights.add(BigDecimal.ONE.subtract(runningSum));
        return weights;
    }

    private void rebalanceSectionWeights(UUID paperId) {
        List<AssessmentPaperSection> sections = assessmentPaperSectionRepository.findByPaperId(paperId).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();
        List<BigDecimal> weights = distributeEqualWeights(sections.size());
        for (int i = 0; i < sections.size(); i++) {
            AssessmentPaperSection section = sections.get(i);
            section.setWeight(weights.get(i));
            assessmentPaperSectionRepository.save(section);
        }
    }
}
