package com.sep.vox.application.port.input.usecase.gradelevel;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.repository.GradeLevelRepository;

@Service
public class UpdateGradeLevelUseCase implements IUseCase<UpdateGradeLevelCommand, UUID> {


    private final GradeLevelRepository gradeLevelRepository;
    private final UserContextPort userContextPort;

    public UpdateGradeLevelUseCase(
            GradeLevelRepository gradeLevelRepository,
            UserContextPort userContextPort) {
        this.gradeLevelRepository = gradeLevelRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateGradeLevelCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        if (gradeLevelRepository.findById(command.gradeLevelId()).isEmpty()) {
            throw new ResourceNotFoundException("Grade level is not found");
        }

        String name = command.name() != null ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        String description = command.description() != null
                ? StringNormalization.trimAndCollapseSpaces(command.description())
                : null;

        int updatedRows = gradeLevelRepository.updateGradeLevelAtomic(
                    command.gradeLevelId(),
                    name,
                    description,
                    command.order(),
                    Instant.now(),
                    currentUserId
            );
        

        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Grade level is not found");
        }

        return command.gradeLevelId();
    }
}
