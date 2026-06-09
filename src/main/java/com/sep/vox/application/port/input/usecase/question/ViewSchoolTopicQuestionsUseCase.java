package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.permission.QuestionCommandPermissionChecker;
import com.sep.vox.application.port.input.query.ViewSchoolTopicQuestionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;

@Service
public class ViewSchoolTopicQuestionsUseCase implements IUseCase<ViewSchoolTopicQuestionsQuery, PageResult<QuestionDto>> {

    private final QuestionReadQueryRepository questionReadQueryRepository;
    private final QuestionCommandPermissionChecker permissionChecker;

    public ViewSchoolTopicQuestionsUseCase(
            QuestionReadQueryRepository questionReadQueryRepository,
            QuestionCommandPermissionChecker permissionChecker) {
        this.questionReadQueryRepository = questionReadQueryRepository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewSchoolTopicQuestionsQuery input) {
        var user = permissionChecker.resolveCurrentUser();
        return questionReadQueryRepository.findSchoolTopicQuestions(
                input.bankId(), input.topicId(), user.schoolId(),
                input.status(), input.keyword(),
                new PageRequest(input.page(), input.size()));
    }
}
