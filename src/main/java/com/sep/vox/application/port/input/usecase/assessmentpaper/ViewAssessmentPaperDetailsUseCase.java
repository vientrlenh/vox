package com.sep.vox.application.port.input.usecase.assessmentpaper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewAssessmentDetailsQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentPaperDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentDetailsUseCase;
import com.sep.vox.domain.dto.assessment.AssessmentPaperDto;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;

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
        var paper = assessmentPaperRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));

        // Tận dụng đúng rule phân quyền xem assessment đã có sẵn — ném ForbiddenException/ResourceNotFoundException
        // nếu người dùng không có quyền xem assessment cha, không viết lại logic phân quyền riêng.
        viewAssessmentDetailsUseCase.execute(new ViewAssessmentDetailsQuery(paper.getAssessmentId()));

        return AssessmentPaperDto.toDto(paper);
    }
}
