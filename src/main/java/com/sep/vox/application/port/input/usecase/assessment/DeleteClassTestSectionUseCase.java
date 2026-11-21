package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteClassTestSectionCommand;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

@Service
public class DeleteClassTestSectionUseCase implements IUseCase<DeleteClassTestSectionCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    public DeleteClassTestSectionUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentPaperSectionRepository assessmentPaperSectionRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentPaperSectionRepository = assessmentPaperSectionRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.recalculateAssessmentTimeDurationService = recalculateAssessmentTimeDurationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentDto execute(DeleteClassTestSectionCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
            throw new ForbiddenException("Chỉ áp dụng cho bài kiểm tra trên lớp");
        }
        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (assessment.getStatus() != AssessmentStatus.DRAFT && assessment.getStatus() != AssessmentStatus.SCHEDULED) {
            throw new IllegalStateException("Chỉ được sửa khi bài kiểm tra chưa bắt đầu");
        }
        if (assessmentRepository.existsSubmittedSessionByAssessmentId(assessment.getId())) {
            throw new IllegalStateException("Không thể sửa câu hỏi khi đã có học sinh nộp bài");
        }
        requireNoAttachedBlueprint(assessment);

        // Suy mã đề từ chính section được xoá: bài trên lớp giờ có thể có nhiều mã đề, đoán "đề đầu
        // tiên" là xoá nhầm section của mã đề khác.
        AssessmentPaperSection paperSection = assessmentPaperSectionRepository.findById(input.sectionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy section"));
        AssessmentPaper paper = assessmentPaperRepository.findById(paperSection.getPaperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        if (!paper.getAssessmentId().equals(assessment.getId())) {
            throw new NotFoundException("Không tìm thấy section");
        }
        List<AssessmentPaperSection> allPaperSections = assessmentPaperSectionRepository.findByPaperId(paper.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();
        if (allPaperSections.size() <= 1) {
            throw new IllegalStateException("Mã đề phải có ít nhất 1 section");
        }

        boolean shouldRebalanceAfterDelete = ClassTestSectionWeightPolicy.looksAutoWeighted(allPaperSections);

        for (AssessmentPaperItem item : assessmentPaperItemRepository.findBySectionId(paperSection.getId())) {
            assessmentPaperItemRepository.deleteById(item.getId());
        }
        assessmentPaperSectionRepository.deleteById(paperSection.getId());

        List<AssessmentPaperSection> remainingPaperSections = allPaperSections.stream().filter(item -> !item.getId().equals(paperSection.getId())).toList();
        for (int i = 0; i < remainingPaperSections.size(); i++) {
            AssessmentPaperSection remaining = remainingPaperSections.get(i);
            int newOrder = i + 1;
            if (remaining.getOrder() != newOrder) {
                remaining.setOrder(newOrder);
            }
            assessmentPaperSectionRepository.save(remaining);
        }
        if (shouldRebalanceAfterDelete) {
            rebalanceSectionWeights(paper.getId());
        } else {
            normalizeSectionWeights(remainingPaperSections);
        }

        Assessment saved = assessmentRepository.save(assessment);
        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentDtoMapper.toDto(saved);
    }

    private void requireNoAttachedBlueprint(Assessment assessment) {
        if (assessment.getBlueprintId() != null) {
            throw new IllegalStateException(
                "Bài đang dùng blueprint dùng chung, không thể sửa câu hỏi trực tiếp — dùng \"Đổi blueprint khác\" ở tab Blueprint để thay đổi cấu trúc");
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
