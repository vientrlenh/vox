package com.sep.vox.application.port.input.usecase.examblueprint;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamBlueprintDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.mapper.ExamBlueprintDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewExamBlueprintDetailsUseCase implements IUseCase<ViewExamBlueprintDetailsQuery, ExamBlueprintDto> {

    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public ViewExamBlueprintDetailsUseCase(
            ExamBlueprintRepository examBlueprintRepository,
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.examBlueprintRepository = examBlueprintRepository;
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamBlueprintDto execute(ViewExamBlueprintDetailsQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !userContextPort.isSystemAdmin()
            && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var blueprint = examBlueprintRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        if (!hasAccess(blueprint, currentUserId, currentSchoolId, schoolAdmin)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return ExamBlueprintDtoMapper.toDto(blueprint);
    }

    private boolean hasAccess(
            ExamBlueprint blueprint,
            UUID currentUserId,
            UUID currentSchoolId,
            boolean schoolAdmin) {
        if (userContextPort.isSystemAdmin()) {
            return true;
        }
        if (currentSchoolId == null || !blueprint.getSchoolId().equals(currentSchoolId)) {
            return false;
        }
        if (schoolAdmin) {
            return true;
        }
        if (blueprint.getCreatedBy().equals(currentUserId)) {
            return true;
        }
        var exams = examRepository.findAllByBlueprintId(blueprint.getId());
        return exams.stream()
            .anyMatch(exam -> examMemberRepository.findByExamIdAndUserId(exam.getId(), currentUserId).isPresent());
    }
}
