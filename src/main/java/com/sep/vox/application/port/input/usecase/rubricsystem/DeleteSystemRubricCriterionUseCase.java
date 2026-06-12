package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSystemRubricCriterionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteSystemRubricCriterionUseCase implements IUseCase<DeleteSystemRubricCriterionCommand, Void> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public DeleteSystemRubricCriterionUseCase(RubricCriterionRepository rubricCriterionRepository, RubricVersionRepository rubricVersionRepository, RubricRepository rubricRepository, UserRepository userRepository, UserContextPort userContextPort) {
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteSystemRubricCriterionCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Lỗi tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        RubricVersion version = rubricVersionRepository.findById(command.versionId()).orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản."));
        if (version.getStatus() != RubricStatus.DRAFT) throw new IllegalStateException("Chỉ xóa được khi là DRAFT.");

        Rubric rubric = rubricRepository.findById(version.getRubricId()).orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) throw new ForbiddenException("Không thể xóa dữ liệu của trường.");

        RubricCriterion criterion = rubricCriterionRepository.findById(command.criterionId()).orElseThrow(() -> new NotFoundException("Không tìm thấy Tiêu chí."));
        if (!criterion.getRubricVersionId().equals(version.getId())) throw new IllegalArgumentException("Tiêu chí sai phiên bản.");

        rubricCriterionRepository.deleteById(criterion.getId());
        return criterion.getId();
    }
}