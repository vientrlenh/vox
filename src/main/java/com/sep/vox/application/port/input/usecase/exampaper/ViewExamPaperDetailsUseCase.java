package com.sep.vox.application.port.input.usecase.exampaper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamDetailsQuery;
import com.sep.vox.application.port.input.query.ViewExamPaperDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.exam.ViewExamDetailsUseCase;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.mapper.ExamPaperDtoMapper;
import com.sep.vox.domain.repository.ExamPaperRepository;

@Service
public class ViewExamPaperDetailsUseCase implements IUseCase<ViewExamPaperDetailsQuery, ExamPaperDto> {

    private final ExamPaperRepository examPaperRepository;
    private final ViewExamDetailsUseCase viewExamDetailsUseCase;

    public ViewExamPaperDetailsUseCase(
            ExamPaperRepository examPaperRepository,
            ViewExamDetailsUseCase viewExamDetailsUseCase) {
        this.examPaperRepository = examPaperRepository;
        this.viewExamDetailsUseCase = viewExamDetailsUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamPaperDto execute(ViewExamPaperDetailsQuery input) {
        var paper = examPaperRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));

        // Tận dụng đúng rule phân quyền xem exam đã có sẵn — ném ForbiddenException/NotFoundException
        // nếu người dùng không có quyền xem exam cha, không viết lại logic phân quyền riêng.
        viewExamDetailsUseCase.execute(new ViewExamDetailsQuery(paper.getExamId()));

        return ExamPaperDtoMapper.toDto(paper);
    }
}
