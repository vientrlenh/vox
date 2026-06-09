package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAdminBankTopicsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionTopicDto;

@Service
public class ViewAdminBankTopicsUseCase implements IUseCase<ViewAdminBankTopicsQuery, PageResult<QuestionTopicDto>> {

    private final QuestionReadQueryRepository questionReadQueryRepository;

    public ViewAdminBankTopicsUseCase(QuestionReadQueryRepository questionReadQueryRepository) {
        this.questionReadQueryRepository = questionReadQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionTopicDto> execute(ViewAdminBankTopicsQuery input) {
        return questionReadQueryRepository.findAdminBankTopics(
                input.bankId(), input.includeArchived(),
                new PageRequest(input.page(), input.size()));
    }
}
