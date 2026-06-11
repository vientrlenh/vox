package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAdminTopicQuestionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;

@Service
public class ViewAdminTopicQuestionsUseCase implements IUseCase<ViewAdminTopicQuestionsQuery, PageResult<QuestionDto>> {

    private final QuestionReadQueryRepository questionReadQueryRepository;

    public ViewAdminTopicQuestionsUseCase(QuestionReadQueryRepository questionReadQueryRepository) {
        this.questionReadQueryRepository = questionReadQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewAdminTopicQuestionsQuery input) {
        return questionReadQueryRepository.findAdminTopicQuestions(
            input.bankId(),
            input.topicId(),
            input.includeArchived(),
            input.scope(),
            input.status(),
            input.type(),
            input.keyword(),
            new PageRequest(input.page(), input.size()));
    }
}
