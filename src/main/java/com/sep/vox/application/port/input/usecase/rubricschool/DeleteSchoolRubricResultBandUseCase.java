package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.DeleteSchoolRubricResultBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSchoolRubricResultBandUseCase implements IUseCase<DeleteSchoolRubricResultBandCommand, Void> {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public Void execute(DeleteSchoolRubricResultBandCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("The account does not exist."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account is locked.");
        }

        RubricVersion version = loadDraftVersionOwnedBySchool(command, currentUserId);
        requireActiveSchool(command.schoolId());

        RubricResultBand resultBand = rubricResultBandRepository.findById(command.resultBandId())
                .orElseThrow(() -> new ResourceNotFoundException("This result band was not found."));
        if (!resultBand.getRubricVersionId().equals(version.getId())) {
            throw new IllegalArgumentException("This result band does not belong to the selected Rubric version.");
        }

        rubricResultBandRepository.deleteById(resultBand.getId());
        return null;
    }

    private RubricVersion loadDraftVersionOwnedBySchool(DeleteSchoolRubricResultBandCommand command, UUID currentUserId) {
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new ResourceNotFoundException("Rubric version not found."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Result bands can only be deleted while the version is in DRAFT status.");
        }

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Your account is not linked to any school."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("You are not allowed to delete data of another school.");
        }
        return version;
    }

    private void requireActiveSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));
        if (!school.isActive()) {
            throw new ForbiddenException("Action denied: this school is disabled in the system.");
        }
    }
}