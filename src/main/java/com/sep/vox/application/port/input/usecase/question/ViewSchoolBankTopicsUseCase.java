package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.permission.QuestionCommandPermissionChecker;
import com.sep.vox.application.port.input.query.ViewSchoolBankTopicsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionTopicDto;

@Service
public class ViewSchoolBankTopicsUseCase implements IUseCase<ViewSchoolBankTopicsQuery, PageResult<QuestionTopicDto>> {

    private final QuestionReadQueryRepository questionReadQueryRepository;
    private final QuestionCommandPermissionChecker permissionChecker;

    public ViewSchoolBankTopicsUseCase(
            QuestionReadQueryRepository questionReadQueryRepository,
            QuestionCommandPermissionChecker permissionChecker) {
        this.questionReadQueryRepository = questionReadQueryRepository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionTopicDto> execute(ViewSchoolBankTopicsQuery input) {
        var user = permissionChecker.resolveCurrentUser();
        return questionReadQueryRepository.findSchoolBankTopics(
                input.bankId(), user.schoolId(),
                new PageRequest(input.page(), input.size()));
    }
}
