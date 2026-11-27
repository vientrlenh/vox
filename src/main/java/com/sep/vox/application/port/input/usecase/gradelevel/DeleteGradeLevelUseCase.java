package com.sep.vox.application.port.input.usecase.gradelevel;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.gradelevel.DeleteGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteGradeLevelUseCase implements IUseCase<DeleteGradeLevelCommand, Void> {

    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteGradeLevelCommand command) {
        requireActiveSystemAdmin();

        GradeLevel gradeLevel = gradeLevelRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("Grade level not found for the given ID."));

        // A grade level that was already soft-deleted (INACTIVE) counts as non-existent.
        // Do NOT deleteById here: this runs inside @Transactional, so the throw rolls back
        // and the hard delete would never really happen (an old bug).
        if (gradeLevel.getStatus() != GradeLevelStatus.ACTIVE) {
            throw new ResourceNotFoundException("Grade level not found.");
        }

        rejectWhenUsedBySchools(gradeLevel);

        // Soft delete: switch to INACTIVE.
        gradeLevel.setStatus(GradeLevelStatus.INACTIVE);
        gradeLevelRepository.save(gradeLevel);

        return null;
    }

    private void requireActiveSystemAdmin() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or has been locked.");
        }
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Only a system admin can delete a grade level.");
        }
    }

    // Blocks deletion while any school grade that is not archived still uses the level. The catalog is shared,
    // so the check scans every school: one school still using it is enough to block.
    private void rejectWhenUsedBySchools(GradeLevel gradeLevel) {
        boolean isUsed = schoolGradeRepository.existsByGradeLevelIdAndStatusNot(
                gradeLevel.getId(), SchoolGradeStatus.ARCHIVED.name());
        if (isUsed) {
            throw new IllegalArgumentException("Cannot delete because schools are using this grade level.");
        }
    }
}
