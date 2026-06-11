package com.sep.vox.application.port.input.usecase.questionbank;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAdminSchoolQuestionBanksQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionBankReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;

@Service
public class ViewAdminSchoolQuestionBanksUseCase implements IUseCase<ViewAdminSchoolQuestionBanksQuery, PageResult<QuestionBankDto>> {

    private final QuestionBankReadQueryRepository questionBankReadQueryRepository;

    public ViewAdminSchoolQuestionBanksUseCase(QuestionBankReadQueryRepository questionBankReadQueryRepository) {
        this.questionBankReadQueryRepository = questionBankReadQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionBankDto> execute(ViewAdminSchoolQuestionBanksQuery input) {
        return questionBankReadQueryRepository.findAdminSchoolQuestionBanks(
            input.schoolId(),
            new PageRequest(input.page(), input.size()));
    }
}
