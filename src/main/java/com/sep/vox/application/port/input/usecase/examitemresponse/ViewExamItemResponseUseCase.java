package com.sep.vox.application.port.input.usecase.examitemresponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.examitemresponse.ExamItemResponseResponseMapper;
import com.sep.vox.application.port.input.query.ViewExamItemResponseQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examitemresponse.ExamItemResponseDetailsResponse;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;

@Service
public class ViewExamItemResponseUseCase implements IUseCase<ViewExamItemResponseQuery, ExamItemResponseDetailsResponse> {

    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;

    public ViewExamItemResponseUseCase(
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemResponseTurnRepository examItemResponseTurnRepository) {
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamItemResponseDetailsResponse execute(ViewExamItemResponseQuery input) {
        var response = examItemResponseRepository.findById(input.answerId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu trả lời của thí sinh"));
        var turns = examItemResponseTurnRepository.findByExamItemResponseId(input.answerId());
        return ExamItemResponseResponseMapper.toDetailsResponse(response, turns);
    }
}
