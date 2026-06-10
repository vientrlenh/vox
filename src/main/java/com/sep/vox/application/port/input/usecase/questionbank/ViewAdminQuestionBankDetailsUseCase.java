package com.sep.vox.application.port.input.usecase.questionbank;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewQuestionBankDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionBankReadQueryRepository;
import com.sep.vox.domain.dto.QuestionBankDto;

@Service
public class ViewAdminQuestionBankDetailsUseCase implements IUseCase<ViewQuestionBankDetailsQuery, QuestionBankDto> {

    private final QuestionBankReadQueryRepository questionBankReadQueryRepository;

    public ViewAdminQuestionBankDetailsUseCase(QuestionBankReadQueryRepository questionBankReadQueryRepository) {
        this.questionBankReadQueryRepository = questionBankReadQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankDto execute(ViewQuestionBankDetailsQuery input) {
        return questionBankReadQueryRepository.findAdminQuestionBank(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));
    }
}
