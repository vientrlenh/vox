package com.sep.vox.application.port.input.usecase.assessmentpaper;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentpaper.UpdateAssessmentPaperSectionCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentPaperSectionDto;
import com.sep.vox.domain.mapper.AssessmentPaperSectionDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentPaperSectionUseCase implements IUseCase<UpdateAssessmentPaperSectionCommand, AssessmentPaperSectionDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentPaperSectionDto execute(UpdateAssessmentPaperSectionCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        AssessmentPaperSection section = assessmentPaperSectionRepository.findById(input.sectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phần trong đề thi"));
        if (!section.getPaperId().equals(input.paperId())) {
            throw new ResourceNotFoundException("Không tìm thấy phần trong đề thi");
        }
        AssessmentPaper paper = assessmentPaperRepository.findById(input.paperId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        Assessment assessment = assessmentRepository.findById(paper.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        if (assessment.getKind() == AssessmentKind.CENTRALIZED && paper.getStatus() == AssessmentPaperStatus.LOCKED) {
            throw new IllegalArgumentException("Đề thi đã bị khoá, không thể sửa");
        }
        if (assessment.getStatus() == AssessmentStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Không thể sửa khi bài kiểm tra đang diễn ra");
        }
        assessmentPaperAuthoringAccessService.requireCanAuthor(assessment, currentUserId);

        if (input.title() != null) {
            section.setTitle(input.title());
        }
        if (input.instruction() != null) {
            section.setInstruction(input.instruction());
        }
        AssessmentPaperSection saved = assessmentPaperSectionRepository.save(section);

        return AssessmentPaperSectionDtoMapper.toDto(saved);
    }
}
