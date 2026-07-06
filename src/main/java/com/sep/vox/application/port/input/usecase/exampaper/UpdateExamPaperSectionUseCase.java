package com.sep.vox.application.port.input.usecase.exampaper;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamPaperSectionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamPaperSectionDto;
import com.sep.vox.domain.mapper.ExamPaperSectionDtoMapper;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;

@Service
public class UpdateExamPaperSectionUseCase implements IUseCase<UpdateExamPaperSectionCommand, ExamPaperSectionDto> {

    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamMemberRepository examMemberRepository;
    private final UserContextPort userContextPort;

    public UpdateExamPaperSectionUseCase(
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamMemberRepository examMemberRepository,
            UserContextPort userContextPort) {
        this.examPaperRepository = examPaperRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examMemberRepository = examMemberRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamPaperSectionDto execute(UpdateExamPaperSectionCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var section = examPaperSectionRepository.findById(input.sectionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phần trong đề thi"));
        if (!section.getPaperId().equals(input.paperId())) {
            throw new NotFoundException("Không tìm thấy phần trong đề thi");
        }
        var paper = examPaperRepository.findById(input.paperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        if (paper.getStatus() == ExamPaperStatus.LOCKED) {
            throw new IllegalStateException("Đề thi đã bị khoá, không thể sửa");
        }
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(paper.getExamId(), currentUserId, ExamMemberRole.AUTHOR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var now = OffsetDateTime.now();
        if (input.title() != null) {
            section.setTitle(input.title());
        }
        if (input.instruction() != null) {
            section.setInstruction(input.instruction());
        }
        section.setUpdatedAt(now);
        section.setUpdatedBy(currentUserId);
        var saved = examPaperSectionRepository.save(section);

        return ExamPaperSectionDtoMapper.toDto(saved);
    }
}
