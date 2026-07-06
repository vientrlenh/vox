package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteClassTestSectionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;

@Service
public class DeleteClassTestSectionUseCase implements IUseCase<DeleteClassTestSectionCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final UserContextPort userContextPort;

    public DeleteClassTestSectionUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
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
        if (exam.getStatus() != ExamStatus.SCHEDULED && exam.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new IllegalStateException("Chỉ được sửa câu hỏi khi bài kiểm tra chưa đóng/hủy");
        }
        if (examRepository.existsSubmittedSessionByExamId(exam.getId())) {
            throw new IllegalStateException("Không thể sửa câu hỏi khi đã có học sinh nộp bài");
        }
        requirePrivateBlueprint(exam);

        var section = examBlueprintSectionRepository.findById(input.sectionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy section"));
        var version = examBlueprintVersionRepository.findById(section.getBlueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        if (!version.getBlueprintId().equals(exam.getBlueprintId())) {
            throw new IllegalStateException("Section không thuộc bài kiểm tra này");
        }

        var allSections = examBlueprintSectionRepository.findByBlueprintVersionId(version.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();
        if (allSections.size() <= 1) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có ít nhất 1 section");
        }

        var paper = examPaperRepository.findByExamId(exam.getId()).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        var allPaperSections = examPaperSectionRepository.findByPaperId(paper.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();
        var paperSection = allPaperSections.stream()
            .filter(candidate -> candidate.getOrder() == section.getOrder())
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy section đề thi tương ứng"));

        for (var item : examPaperItemRepository.findBySectionId(paperSection.getId())) {
            examPaperItemRepository.deleteById(item.getId());
        }
        examPaperSectionRepository.deleteById(paperSection.getId());

        for (var slot : examBlueprintSlotRepository.findBySectionId(section.getId())) {
            examBlueprintSlotRepository.deleteById(slot.getId());
        }
        examBlueprintSectionRepository.deleteById(section.getId());

        var now = OffsetDateTime.now();
        var remainingSections = allSections.stream().filter(item -> !item.getId().equals(section.getId())).toList();
        for (int i = 0; i < remainingSections.size(); i++) {
            var remaining = remainingSections.get(i);
            var newOrder = i + 1;
            if (remaining.getOrder() != newOrder) {
                remaining.setOrder(newOrder);
                remaining.setUpdatedAt(now);
                remaining.setUpdatedBy(currentUserId);
                examBlueprintSectionRepository.save(remaining);
            }
        }

        var remainingPaperSections = allPaperSections.stream().filter(item -> !item.getId().equals(paperSection.getId())).toList();
        for (int i = 0; i < remainingPaperSections.size(); i++) {
            var remaining = remainingPaperSections.get(i);
            var newOrder = i + 1;
            if (remaining.getOrder() != newOrder) {
                remaining.setOrder(newOrder);
                remaining.setUpdatedAt(now);
                remaining.setUpdatedBy(currentUserId);
                examPaperSectionRepository.save(remaining);
            }
        }

        exam.setUpdatedAt(now);
        exam.setUpdatedBy(currentUserId);
        var saved = examRepository.save(exam);
        return ExamDtoMapper.toDto(saved);
    }

    private void requirePrivateBlueprint(Exam exam) {
        boolean sharedWithOtherExam = examRepository.findAllByBlueprintId(exam.getBlueprintId()).stream()
            .anyMatch(other -> !other.getId().equals(exam.getId()));
        if (sharedWithOtherExam) {
            throw new IllegalStateException(
                "Blueprint đang được dùng chung cho kỳ thi/bài kiểm tra khác, không thể sửa câu hỏi trực tiếp ở đây");
        }
    }
}
