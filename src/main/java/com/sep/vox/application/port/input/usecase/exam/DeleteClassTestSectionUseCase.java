package com.sep.vox.application.port.input.usecase.exam;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteClassTestSectionCommand;
import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;

@Service
public class DeleteClassTestSectionUseCase implements IUseCase<DeleteClassTestSectionCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final RecalculateExamTimeDurationService recalculateExamTimeDurationService;
    private final UserContextPort userContextPort;

    public DeleteClassTestSectionUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            RecalculateExamTimeDurationService recalculateExamTimeDurationService,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.recalculateExamTimeDurationService = recalculateExamTimeDurationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamDto execute(DeleteClassTestSectionCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        if (exam.getKind() != ExamKind.CLASS_TEST) {
            throw new ForbiddenException("Chỉ áp dụng cho bài kiểm tra trên lớp");
        }
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.SCHEDULED) {
            throw new IllegalStateException("Chỉ được sửa khi bài kiểm tra chưa bắt đầu");
        }
        if (examRepository.existsSubmittedSessionByExamId(exam.getId())) {
            throw new IllegalStateException("Không thể sửa câu hỏi khi đã có học sinh nộp bài");
        }
        requireNoAttachedBlueprint(exam);

        // Suy mã đề từ chính section được xoá: bài trên lớp giờ có thể có nhiều mã đề, đoán "đề đầu
        // tiên" là xoá nhầm section của mã đề khác.
        var paperSection = examPaperSectionRepository.findById(input.sectionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy section"));
        var paper = examPaperRepository.findById(paperSection.getPaperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        if (!paper.getExamId().equals(exam.getId())) {
            throw new NotFoundException("Không tìm thấy section");
        }
        var allPaperSections = examPaperSectionRepository.findByPaperId(paper.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();
        if (allPaperSections.size() <= 1) {
            throw new IllegalStateException("Mã đề phải có ít nhất 1 section");
        }

        var shouldRebalanceAfterDelete = ClassTestSectionWeightPolicy.looksAutoWeighted(allPaperSections);

        for (var item : examPaperItemRepository.findBySectionId(paperSection.getId())) {
            examPaperItemRepository.deleteById(item.getId());
        }
        examPaperSectionRepository.deleteById(paperSection.getId());

        var now = Instant.now();
        var remainingPaperSections = allPaperSections.stream().filter(item -> !item.getId().equals(paperSection.getId())).toList();
        for (int i = 0; i < remainingPaperSections.size(); i++) {
            var remaining = remainingPaperSections.get(i);
            var newOrder = i + 1;
            if (remaining.getOrder() != newOrder) {
                remaining.setOrder(newOrder);
            }
            remaining.setUpdatedAt(now);
            remaining.setUpdatedBy(currentUserId);
            examPaperSectionRepository.save(remaining);
        }
        if (shouldRebalanceAfterDelete) {
            rebalanceSectionWeights(paper.getId(), now, currentUserId);
        } else {
            normalizeSectionWeights(remainingPaperSections, now, currentUserId);
        }

        exam.setUpdatedAt(now);
        exam.setUpdatedBy(currentUserId);
        var saved = examRepository.save(exam);
        recalculateExamTimeDurationService.recalculate(exam.getId());
        return ExamDtoMapper.toDto(saved);
    }

    private void requireNoAttachedBlueprint(Exam exam) {
        if (exam.getBlueprintId() != null) {
            throw new IllegalStateException(
                "Bài đang dùng blueprint dùng chung, không thể sửa câu hỏi trực tiếp — dùng \"Đổi blueprint khác\" ở tab Blueprint để thay đổi cấu trúc");
        }
    }
    private void rebalanceSectionWeights(UUID paperId, Instant now, UUID currentUserId) {
        var sections = examPaperSectionRepository.findByPaperId(paperId).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();
        var weights = distributeEqualWeights(sections.size());
        for (int i = 0; i < sections.size(); i++) {
            var section = sections.get(i);
            section.setWeight(weights.get(i));
            section.setUpdatedAt(now);
            section.setUpdatedBy(currentUserId);
            examPaperSectionRepository.save(section);
        }
    }

    private void normalizeSectionWeights(List<com.sep.vox.domain.model.exam.ExamPaperSection> sections, Instant now, UUID currentUserId) {
        var weights = ClassTestSectionWeightPolicy.normalizeStoredWeights(sections);
        for (int i = 0; i < sections.size(); i++) {
            var section = sections.get(i);
            section.setWeight(weights.get(i));
            section.setUpdatedAt(now);
            section.setUpdatedBy(currentUserId);
            examPaperSectionRepository.save(section);
        }
    }

    private List<BigDecimal> distributeEqualWeights(int count) {
        var weights = new ArrayList<BigDecimal>();
        var perItem = BigDecimal.ONE.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.DOWN);
        var runningSum = BigDecimal.ZERO;
        for (int i = 0; i < count - 1; i++) {
            weights.add(perItem);
            runningSum = runningSum.add(perItem);
        }
        weights.add(BigDecimal.ONE.subtract(runningSum));
        return weights;
    }
}
