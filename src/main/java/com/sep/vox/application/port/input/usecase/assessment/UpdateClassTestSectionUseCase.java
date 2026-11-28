package com.sep.vox.application.port.input.usecase.assessment;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.ClassTestQuestionCommand;
import com.sep.vox.application.port.input.command.assessment.UpdateClassTestSectionCommand;
import com.sep.vox.application.port.input.service.AssessmentQuestionSecureLockService;
import com.sep.vox.application.port.input.service.ClassTestDirectEditGuard;
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
public class UpdateClassTestSectionUseCase implements IUseCase<UpdateClassTestSectionCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final ClassTestDirectEditGuard classTestDirectEditGuard;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentDto execute(UpdateClassTestSectionCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        classTestDirectEditGuard.requireDirectlyEditable(assessment, currentUserId);

        AssessmentPaperSection paperSection = assessmentPaperSectionRepository.findById(input.sectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        applySectionFields(paperSection, input);

        if (input.questions() != null) {
            replaceSectionQuestions(paperSection, input.questions(), assessment, currentUserId);
        }

        Assessment saved = assessmentRepository.save(assessment);
        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentDtoMapper.toDto(saved);
    }

    private void applySectionFields(AssessmentPaperSection paperSection, UpdateClassTestSectionCommand input) {
        if (input.title() != null) {
            paperSection.setTitle(input.title());
            assessmentPaperSectionRepository.save(paperSection);
        }

        if (input.instruction() != null) {
            paperSection.setInstruction(input.instruction());
            assessmentPaperSectionRepository.save(paperSection);
        }

        if (input.weight() != null) {
            paperSection.setWeight(input.weight());
            List<AssessmentPaperSection> sections = assessmentPaperSectionRepository.findByPaperId(paperSection.getPaperId()).stream()
                .map(section -> section.getId().equals(paperSection.getId()) ? paperSection : section)
                .toList();
            ClassTestSectionWeightPolicy.validateStoredWeights(sections, "The total section weight must be 1.00");
            assessmentPaperSectionRepository.save(paperSection);
        }
    }

    private void replaceSectionQuestions(
            AssessmentPaperSection paperSection,
            List<ClassTestQuestionCommand> questions,
            Assessment assessment,
            UUID currentUserId) {
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("A section must have at least 1 question");
        }
        for (ClassTestQuestionCommand questionCommand : questions) {
            classTestDirectEditGuard.loadUsableQuestion(assessment, questionCommand.questionId(), currentUserId);
        }
        updateSectionQuestions(paperSection, questions, assessment.getId(), currentUserId);
    }

    private void updateSectionQuestions(
            AssessmentPaperSection paperSection,
            List<ClassTestQuestionCommand> questions,
            UUID assessmentId,
            UUID currentUserId) {
        List<AssessmentPaperItem> existingItems = assessmentPaperItemRepository.findBySectionId(paperSection.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();
        for (AssessmentPaperItem item : existingItems) {
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
        assessmentPaperSectionRepository.save(paperSection);
    }
}
