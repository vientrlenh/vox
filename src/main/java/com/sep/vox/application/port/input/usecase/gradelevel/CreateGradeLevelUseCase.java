package com.sep.vox.application.port.input.usecase.gradelevel;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;


@Service
public class CreateGradeLevelUseCase implements IUseCase<CreateGradeLevelCommand, UUID> {

    private final GradeLevelRepository gradeLevelRepository;
    private final UserContextPort userContextPort;

    public CreateGradeLevelUseCase(
            GradeLevelRepository gradeLevelRepository,
            UserContextPort userContextPort) {
        this.gradeLevelRepository = gradeLevelRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(CreateGradeLevelCommand input) {
        CreateGradeLevelCommand command = normalize(input);

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        validateUniqueCodeAndOrder(command.code(), command.order());

        return saveNewGradeLevel(command, normalizedCode, currentUserId);
    }

    private CreateGradeLevelCommand normalize(CreateGradeLevelCommand input) {
        return new CreateGradeLevelCommand(
            StringNormalization.normalizeCode(input.code()), 
            StringNormalization.trimAndCollapseSpaces(input.name()), 
            StringNormalization.trimAndCollapseSpaces(input.description()), 
            input.order()
        );
    }

    private void validateUniqueCodeAndOrder(String code, Integer order) {
        if (gradeLevelRepository.existsByCode(code)) {
            throw new ResourceDuplicatedException("Grade level code is already in use");
        }
        if (order != null && gradeLevelRepository.existsByOrder(order)) {
            throw new ResourceDuplicatedException("Grade level order is already in use");
        }
    }

    private UUID saveNewGradeLevel(CreateGradeLevelCommand command, String code, UUID creatorId) {
        GradeLevel newGradeLevel = GradeLevel.create(name, code, null, creatorId);
        try {
            return gradeLevelRepository.save(newGradeLevel).getId();
        } catch (DataIntegrityViolationException e) {
            // Chống race-condition: hai request cùng tạo trùng mã/thứ tự vượt qua check exists rồi mới đụng unique index.
            throw new DuplicatedException("Mã hoặc thứ tự khối học đã tồn tại.");
        }
    }
}
