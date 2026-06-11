package com.sep.vox.application.port.input.usecase.questiontopic;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewQuestionTopicDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewTeacherQuestionTopicDetailsUseCase implements IUseCase<ViewQuestionTopicDetailsQuery, QuestionTopicDto> {

    private final QuestionReadQueryRepository questionReadQueryRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    public ViewTeacherQuestionTopicDetailsUseCase(
            QuestionReadQueryRepository questionReadQueryRepository,
            UserContextPort userContextPort,
            UserRepository userRepository) {
        this.questionReadQueryRepository = questionReadQueryRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionTopicDto execute(ViewQuestionTopicDetailsQuery input) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng"));
        return questionReadQueryRepository.findTeacherTopicDetail(input.id(), userId, user.getSchoolId())
            .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y chá»§ Ä‘á» cÃ¢u há»i"));
    }
}
