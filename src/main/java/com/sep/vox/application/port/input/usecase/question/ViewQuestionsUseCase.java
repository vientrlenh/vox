package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.Question;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewQuestionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class ViewQuestionsUseCase implements IUseCase<ViewQuestionsQuery, PageResult<QuestionDto>> {

    private final QuestionRepository questionRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewQuestionsQuery input) {
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

        PageResult<Question> result = questionRepository.findAccessible(
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            input.questionBankId(),
            input.questionTopicId(),
            input.topicName(),
            input.status(),
            input.type(),
            input.sharing(),
            input.assetType(),
            input.scope(),
            input.keyword(),
            input.page(),
            input.size()
        );
        return QuestionDtoMapper.toDtoPage(result);
    }
}
