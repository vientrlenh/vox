package com.sep.vox.application.port.input.usecase.questiontopic;

import com.sep.vox.domain.model.question.QuestionTopic;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewQuestionTopicsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.mapper.QuestionTopicDtoMapper;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class ViewQuestionTopicsUseCase implements IUseCase<ViewQuestionTopicsQuery, PageResult<QuestionTopicDto>> {

    private final QuestionTopicRepository questionTopicRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionTopicDto> execute(ViewQuestionTopicsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        boolean systemAdmin = userContextPort.isSystemAdmin();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (!systemAdmin && currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        PageResult<QuestionTopic> result = questionTopicRepository.findAccessible(
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            input.questionBankId(),
            input.status(),
            input.keyword(),
            input.page(),
            input.size()
        );
        return QuestionTopicDtoMapper.toDtoPage(result);
    }
}
