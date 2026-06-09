package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.permission.QuestionCommandPermissionChecker;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewQuestionDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.dto.QuestionDetailDto;

@Service
public class ViewQuestionDetailUseCase implements IUseCase<ViewQuestionDetailsQuery, QuestionDetailDto> {

    private final QuestionReadQueryRepository questionReadQueryRepository;
    private final QuestionCommandPermissionChecker permissionChecker;

    public ViewQuestionDetailUseCase(
            QuestionReadQueryRepository questionReadQueryRepository,
            QuestionCommandPermissionChecker permissionChecker) {
        this.questionReadQueryRepository = questionReadQueryRepository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDetailDto execute(ViewQuestionDetailsQuery input) {
        var user = permissionChecker.resolveCurrentUser();
        return questionReadQueryRepository.findVisibleQuestionDetail(
                input.id(), user.userId(), user.role().name(), user.schoolId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
    }
}
