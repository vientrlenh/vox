package com.sep.vox.application.port.input.usecase.questionbank;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewQuestionBankDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.mapper.QuestionBankDtoMapper;
import com.sep.vox.domain.repository.QuestionBankRepository;

@Service
public class ViewQuestionBankDetailsUseCase implements IUseCase<ViewQuestionBankDetailsQuery, QuestionBankDto> {

    private final QuestionBankRepository questionBankRepository;

    public ViewQuestionBankDetailsUseCase(QuestionBankRepository questionBankRepository) {
        this.questionBankRepository = questionBankRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankDto execute(ViewQuestionBankDetailsQuery input) {
        var questionBank = questionBankRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));
        return QuestionBankDtoMapper.toDto(questionBank);
    }
}
