package com.sep.vox.application.port.input.usecase.assessmentpaper;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentPaperSectionCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentPaperSectionDto;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

@Service
public class UpdateAssessmentPaperSectionUseCase implements IUseCase<UpdateAssessmentPaperSectionCommand, AssessmentPaperSectionDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService;
    private final UserContextPort userContextPort;

    public UpdateAssessmentPaperSectionUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentPaperSectionRepository assessmentPaperSectionRepository,
            AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentPaperSectionRepository = assessmentPaperSectionRepository;
        this.assessmentPaperAuthoringAccessService = assessmentPaperAuthoringAccessService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentPaperSectionDto execute(UpdateAssessmentPaperSectionCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var section = assessmentPaperSectionRepository.findById(input.sectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phần trong đề thi"));
        if (!section.getPaperId().equals(input.paperId())) {
            throw new ResourceNotFoundException("Không tìm thấy phần trong đề thi");
        }
        var paper = assessmentPaperRepository.findById(input.paperId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        var assessment = assessmentRepository.findById(paper.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        if (assessment.getKind() == AssessmentKind.CENTRALIZED && paper.getStatus() == AssessmentPaperStatus.LOCKED) {
            throw new IllegalStateException("Đề thi đã bị khoá, không thể sửa");
        }
        if (assessment.getStatus() == AssessmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Không thể sửa khi bài kiểm tra đang diễn ra");
        }
        assessmentPaperAuthoringAccessService.requireCanAuthor(assessment, currentUserId);

        var now = Instant.now();
        if (input.title() != null) {
            section.setTitle(input.title());
        }
        if (input.instruction() != null) {
            section.setInstruction(input.instruction());
        }
        section.setUpdatedAt(now);
        section.setUpdatedBy(currentUserId);
        var saved = assessmentPaperSectionRepository.save(section);

        return AssessmentPaperSectionDto.toDto(saved);
    }
}
