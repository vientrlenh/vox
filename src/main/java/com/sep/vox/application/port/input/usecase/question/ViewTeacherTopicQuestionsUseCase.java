package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.permission.QuestionCommandPermissionChecker;
import com.sep.vox.application.port.input.query.ViewTeacherTopicQuestionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;

@Service
public class ViewTeacherTopicQuestionsUseCase implements IUseCase<ViewTeacherTopicQuestionsQuery, PageResult<QuestionDto>> {

    private final QuestionReadQueryRepository questionReadQueryRepository;
    private final QuestionCommandPermissionChecker permissionChecker;

    public ViewTeacherTopicQuestionsUseCase(
            QuestionReadQueryRepository questionReadQueryRepository,
            QuestionCommandPermissionChecker permissionChecker) {
        this.questionReadQueryRepository = questionReadQueryRepository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewTeacherTopicQuestionsQuery input) {
        var user = permissionChecker.resolveCurrentUser();
        return questionReadQueryRepository.findTeacherTopicQuestions(
                input.bankId(), input.topicId(), user.userId(), user.schoolId(),
                input.status(), input.keyword(),
                new PageRequest(input.page(), input.size()));
    }
}
