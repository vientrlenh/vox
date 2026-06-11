package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAdminBankQuestionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;

@Service
public class ViewAdminBankQuestionsUseCase implements IUseCase<ViewAdminBankQuestionsQuery, PageResult<QuestionDto>> {

    private final QuestionReadQueryRepository questionReadQueryRepository;

    public ViewAdminBankQuestionsUseCase(QuestionReadQueryRepository questionReadQueryRepository) {
        this.questionReadQueryRepository = questionReadQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewAdminBankQuestionsQuery input) {
        return questionReadQueryRepository.findAdminBankQuestions(
                input.bankId(), input.includeArchived(), input.scope(), input.status(), input.type(), input.keyword(),
                new PageRequest(input.page(), input.size()));
    }
}
