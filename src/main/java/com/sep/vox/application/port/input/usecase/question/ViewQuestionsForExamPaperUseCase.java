package com.sep.vox.application.port.input.usecase.question;

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
public class ViewQuestionsForExamPaperUseCase implements IUseCase<ViewQuestionsQuery, PageResult<QuestionDto>> {

    private final QuestionRepository questionRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public ViewQuestionsForExamPaperUseCase(QuestionRepository questionRepository, UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository, UserRoleQueryRepository userRoleQueryRepository) {
        this.questionRepository = questionRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewQuestionsQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var systemAdmin = userContextPort.isSystemAdmin();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (!systemAdmin && currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var result = questionRepository.findAccessibleForExamPaper(
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
            input.scope(),
            input.keyword(),
            input.page(),
            input.size()
        );
        return QuestionDtoMapper.toDtoPage(result);
    }
}
