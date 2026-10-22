package com.sep.vox.application.port.input.usecase.questiontopic;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewQuestionTopicsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.shared.PageResult;

@Service
public class ViewQuestionTopicsUseCase implements IUseCase<ViewQuestionTopicsQuery, PageResult<QuestionTopicDto>> {

    private final QuestionTopicRepository questionTopicRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public ViewQuestionTopicsUseCase(QuestionTopicRepository questionTopicRepository, UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.questionTopicRepository = questionTopicRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionTopicDto> execute(ViewQuestionTopicsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var systemAdmin = userContextPort.isSystemAdmin();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !systemAdmin && userContextPort.isSchoolAdmin();

        if (!systemAdmin && currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var result = questionTopicRepository.findAccessible(
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            input.questionBankId(),
            input.status(),
            input.keyword(),
            input.pageRequest().repositoryPage(),
            input.pageRequest().size()
        );
        return QuestionTopicDto.toDtoPage(result);
    }
}
