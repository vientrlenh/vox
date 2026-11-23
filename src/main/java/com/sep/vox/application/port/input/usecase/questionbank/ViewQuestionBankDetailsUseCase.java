package com.sep.vox.application.port.input.usecase.questionbank;

import com.sep.vox.domain.model.question.QuestionBank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.questionbank.ViewQuestionBankDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.mapper.QuestionBankDtoMapper;
import com.sep.vox.domain.repository.QuestionBankRepository;

@Service
@RequiredArgsConstructor
public class ViewQuestionBankDetailsUseCase implements IUseCase<ViewQuestionBankDetailsQuery, QuestionBankDto> {

    private final QuestionBankRepository questionBankRepository;

    @Override
    @Transactional(readOnly = true)
    public QuestionBankDto execute(ViewQuestionBankDetailsQuery input) {
        QuestionBank questionBank = questionBankRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));
        return QuestionBankDtoMapper.toDto(questionBank);
    }
}
