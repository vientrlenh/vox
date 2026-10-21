package com.sep.vox.application.port.input.usecase.questionbank;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewQuestionBanksQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.shared.PageResult;

@Service
public class ViewQuestionBanksUseCase implements IUseCase<ViewQuestionBanksQuery, PageResult<QuestionBankDto>> {

    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public ViewQuestionBanksUseCase(QuestionBankRepository questionBankRepository, UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository, UserRoleQueryRepository userRoleQueryRepository) {
        this.questionBankRepository = questionBankRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionBankDto> execute(ViewQuestionBanksQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var systemAdmin = userContextPort.isSystemAdmin();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (!systemAdmin && currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var result = questionBankRepository.findAccessible(
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            input.ownerType(),
            input.status(),
            input.languageId(),
            input.schoolId(),
            input.schoolGradeId(),
            input.keyword(),
            input.page(),
            input.size()
        );
        return QuestionBankDto.toDtoPage(result);
    }
}
