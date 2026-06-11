package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewTeacherQuestionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewTeacherQuestionsUseCase implements IUseCase<ViewTeacherQuestionsQuery, PageResult<QuestionDto>> {

    private final QuestionReadQueryRepository questionReadQueryRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    public ViewTeacherQuestionsUseCase(
            QuestionReadQueryRepository questionReadQueryRepository,
            UserContextPort userContextPort,
            UserRepository userRepository) {
        this.questionReadQueryRepository = questionReadQueryRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewTeacherQuestionsQuery input) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng"));
        return questionReadQueryRepository.findTeacherVisibleQuestions(
            userId,
            user.getSchoolId(),
            input.scope(),
            input.status(),
            input.type(),
            input.keyword(),
            new PageRequest(input.page(), input.size()));
    }
}
