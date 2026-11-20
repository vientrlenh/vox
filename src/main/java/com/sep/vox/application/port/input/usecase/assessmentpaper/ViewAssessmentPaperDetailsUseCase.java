package com.sep.vox.application.port.input.usecase.assessmentpaper;

import com.sep.vox.domain.model.assessment.AssessmentPaper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewAssessmentDetailsQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentPaperDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentDetailsUseCase;
import com.sep.vox.domain.dto.AssessmentPaperDto;
import com.sep.vox.domain.mapper.AssessmentPaperDtoMapper;
import com.sep.vox.domain.repository.AssessmentPaperRepository;

@Service
public class ViewAssessmentPaperDetailsUseCase implements IUseCase<ViewAssessmentPaperDetailsQuery, AssessmentPaperDto> {

    private final AssessmentPaperRepository assessmentPaperRepository;
    private final ViewAssessmentDetailsUseCase viewAssessmentDetailsUseCase;

    public ViewAssessmentPaperDetailsUseCase(
            AssessmentPaperRepository assessmentPaperRepository,
            ViewAssessmentDetailsUseCase viewAssessmentDetailsUseCase) {
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.viewAssessmentDetailsUseCase = viewAssessmentDetailsUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentPaperDto execute(ViewAssessmentPaperDetailsQuery input) {
        AssessmentPaper paper = assessmentPaperRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));

        // Reuses exactly the existing permission rule for viewing an assessment -- throws ForbiddenException/NotFoundException
        // if the user has no right to view the parent assessment, without rewriting separate permission logic.
        viewAssessmentDetailsUseCase.execute(new ViewAssessmentDetailsQuery(paper.getAssessmentId()));

        return AssessmentPaperDtoMapper.toDto(paper);
    }
}
