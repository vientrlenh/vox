package com.sep.vox.application.port.input.usecase.examitemresponse;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.examitemresponse.ExamItemResponseResponseMapper;
import com.sep.vox.application.port.input.query.ViewExamItemResponseTurnsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examitemresponse.ExamItemResponseTurnResponse;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;

@Service
public class ViewExamItemResponseTurnsUseCase implements IUseCase<ViewExamItemResponseTurnsQuery, List<ExamItemResponseTurnResponse>> {

    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;

    public ViewExamItemResponseTurnsUseCase(
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemResponseTurnRepository examItemResponseTurnRepository) {
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamItemResponseTurnResponse> execute(ViewExamItemResponseTurnsQuery input) {
        if (!examItemResponseRepository.existsById(input.answerId())) {
            throw new NotFoundException("Không tìm thấy câu trả lời của thí sinh");
        }

        return examItemResponseTurnRepository.findByExamItemResponseId(input.answerId()).stream()
            .map(ExamItemResponseResponseMapper::toTurnResponse)
            .toList();
    }
}
