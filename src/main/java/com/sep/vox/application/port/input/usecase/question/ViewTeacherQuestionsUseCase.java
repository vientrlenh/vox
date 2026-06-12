package com.sep.vox.application.port.input.usecase.question;

import java.util.UUID;

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
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewTeacherQuestionsUseCase implements IUseCase<ViewTeacherQuestionsQuery, PageResult<QuestionDto>> {

    private final QuestionReadQueryRepository questionReadQueryRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    public ViewTeacherQuestionsUseCase(
            QuestionReadQueryRepository questionReadQueryRepository,
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository) {
        this.questionReadQueryRepository = questionReadQueryRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewTeacherQuestionsQuery input) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("KhÃƒÂ´ng tÃƒÂ¬m thÃ¡ÂºÂ¥y ngÃ†Â°Ã¡Â»Âi dÃƒÂ¹ng"));
        return questionReadQueryRepository.findTeacherVisibleQuestions(
            userId,
            getSchoolId(user.getId()),
            input.scope(),
            input.status(),
            input.type(),
            input.keyword(),
            new PageRequest(input.page(), input.size()));
    }

    private UUID getSchoolId(UUID userId) {
        return schoolUserRepository.findByUserId(userId)
            .map(SchoolUser::getSchoolId)
            .orElseThrow(() -> new IllegalStateException("Nguoi dung hien tai khong thuoc truong nao"));
    }
}
