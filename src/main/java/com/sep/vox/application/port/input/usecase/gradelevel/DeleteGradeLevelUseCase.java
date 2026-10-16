package com.sep.vox.application.port.input.usecase.gradelevel;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteGradeLevelUseCase implements IUseCase<DeleteGradeLevelCommand, Void> {

    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final UserContextPort userContextPort;

    public DeleteGradeLevelUseCase(
            GradeLevelRepository gradeLevelRepository,
            SchoolGradeRepository schoolGradeRepository,
            UserContextPort userContextPort) {
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteGradeLevelCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        GradeLevel gradeLevel = gradeLevelRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("Grade level is not found"));

        if (gradeLevel.isNotActive()) {
            throw new ResourceNotFoundException("Grade level is not active to be deleted");
        }

        boolean isUsed = schoolGradeRepository.existsByGradeLevelIdAndStatusNot(
                gradeLevel.getId(), SchoolGradeStatus.ARCHIVED.name());
        if (isUsed) {
            throw new IllegalStateException("Grade level cannot be deleted since there are schools still using it.");
        }

        gradeLevel.delete(currentUserId);
        gradeLevelRepository.save(gradeLevel);

        return null;
    }
}
