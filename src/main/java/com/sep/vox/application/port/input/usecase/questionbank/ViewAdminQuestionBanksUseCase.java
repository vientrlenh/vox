package com.sep.vox.application.port.input.usecase.questionbank;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAdminQuestionBanksQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionBankReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;

@Service
public class ViewAdminQuestionBanksUseCase implements IUseCase<ViewAdminQuestionBanksQuery, PageResult<QuestionBankDto>> {

    private final QuestionBankReadQueryRepository questionBankReadQueryRepository;

    public ViewAdminQuestionBanksUseCase(QuestionBankReadQueryRepository questionBankReadQueryRepository) {
        this.questionBankReadQueryRepository = questionBankReadQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionBankDto> execute(ViewAdminQuestionBanksQuery input) {
        return questionBankReadQueryRepository.findAdminQuestionBanks(new PageRequest(input.page(), input.size()));
    }
}
