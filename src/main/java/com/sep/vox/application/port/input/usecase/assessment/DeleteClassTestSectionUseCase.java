package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.DeleteClassTestSectionCommand;
import com.sep.vox.application.port.input.service.ClassTestDirectEditGuard;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

@Service
@RequiredArgsConstructor
public class DeleteClassTestSectionUseCase implements IUseCase<DeleteClassTestSectionCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final ClassTestDirectEditGuard classTestDirectEditGuard;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentDto execute(DeleteClassTestSectionCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        classTestDirectEditGuard.requireDirectlyEditable(assessment, currentUserId);

        // The paper is derived from the section being deleted: a class test can now have several papers,
        // and guessing "the first paper" would delete a section of another paper by mistake.
        AssessmentPaperSection paperSection = assessmentPaperSectionRepository.findById(input.sectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        AssessmentPaper paper = requirePaperOfAssessment(paperSection, assessment);
        List<AssessmentPaperSection> allPaperSections = requireMoreThanOneSection(paper);

        boolean shouldRebalanceAfterDelete = ClassTestSectionWeightPolicy.looksAutoWeighted(allPaperSections);

        deleteSectionWithItems(paperSection);
        List<AssessmentPaperSection> remainingPaperSections = allPaperSections.stream().filter(item -> !item.getId().equals(paperSection.getId())).toList();
        renumberSections(remainingPaperSections);
        reweightRemainingSections(shouldRebalanceAfterDelete, paper, remainingPaperSections);

        Assessment saved = assessmentRepository.save(assessment);
        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentDtoMapper.toDto(saved);
    }

    private void reweightRemainingSections(
            boolean shouldRebalance,
            AssessmentPaper paper,
            List<AssessmentPaperSection> remainingPaperSections) {
        if (shouldRebalance) {
            rebalanceSectionWeights(paper.getId());
        } else {
            normalizeSectionWeights(remainingPaperSections);
        }
    }

    private AssessmentPaper requirePaperOfAssessment(AssessmentPaperSection paperSection, Assessment assessment) {
        AssessmentPaper paper = assessmentPaperRepository.findById(paperSection.getPaperId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment paper not found"));
        if (!paper.getAssessmentId().equals(assessment.getId())) {
            throw new ResourceNotFoundException("Section not found");
        }
        return paper;
    }

    private List<AssessmentPaperSection> requireMoreThanOneSection(AssessmentPaper paper) {
        List<AssessmentPaperSection> allPaperSections = assessmentPaperSectionRepository.findByPaperId(paper.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();
        if (allPaperSections.size() <= 1) {
            throw new IllegalArgumentException("A paper must have at least 1 section");
        }
        return allPaperSections;
    }

    private void deleteSectionWithItems(AssessmentPaperSection paperSection) {
        for (AssessmentPaperItem item : assessmentPaperItemRepository.findBySectionId(paperSection.getId())) {
            assessmentPaperItemRepository.deleteById(item.getId());
        }
        assessmentPaperSectionRepository.deleteById(paperSection.getId());
    }

    private void renumberSections(List<AssessmentPaperSection> remainingPaperSections) {
        for (int i = 0; i < remainingPaperSections.size(); i++) {
            AssessmentPaperSection remaining = remainingPaperSections.get(i);
            int newOrder = i + 1;
            if (remaining.getOrder() != newOrder) {
                remaining.setOrder(newOrder);
            }
            assessmentPaperSectionRepository.save(remaining);
        }
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

    private void normalizeSectionWeights(List<com.sep.vox.domain.model.assessment.AssessmentPaperSection> sections) {
        List<BigDecimal> weights = ClassTestSectionWeightPolicy.normalizeStoredWeights(sections);
        for (int i = 0; i < sections.size(); i++) {
            AssessmentPaperSection section = sections.get(i);
            section.setWeight(weights.get(i));
            assessmentPaperSectionRepository.save(section);
        }
    }

    private List<BigDecimal> distributeEqualWeights(int count) {
        List<BigDecimal> weights = new ArrayList<BigDecimal>();
        BigDecimal perItem = BigDecimal.ONE.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.DOWN);
        BigDecimal runningSum = BigDecimal.ZERO;
        for (int i = 0; i < count - 1; i++) {
            weights.add(perItem);
            runningSum = runningSum.add(perItem);
        }
        weights.add(BigDecimal.ONE.subtract(runningSum));
        return weights;
    }
}
