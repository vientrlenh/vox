package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.CreateSchoolRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
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
public class CreateSchoolRubricUseCase implements IUseCase<CreateSchoolRubricCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;
    private final SupportedLanguageRepository languageRepository;
    private final FrameworkRepository frameworkRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(CreateSchoolRubricCommand command) {
        requireActiveSchoolMember(command);
        requireActiveLanguageAndFramework(command);

        String safeCode = StringNormalization.trimAndCollapseSpaces(command.code());
        String safeName = StringNormalization.trimAndCollapseSpaces(command.name());
        String safeDesc = command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        requireCodeNotTaken(command, safeCode);

        Rubric newRubric = Rubric.forSchool(
                command.languageId(), command.frameworkId(), safeCode, safeName, safeDesc, command.schoolId());
        Rubric savedRubric = rubricRepository.save(newRubric);
        return savedRubric.getId();
    }

    private void requireActiveSchoolMember(CreateSchoolRubricCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account has been locked.");
        }

        // Strict school membership check to prevent cross-tenant data leaks.
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Your account is not assigned to any school."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("SECURITY: you are not allowed to create a Rubric for another school.");
        }

        School school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School information not found."));
        if (!school.isActive()) {
            throw new IllegalArgumentException("The school is locked, so this operation cannot be performed.");
        }
    }

    private void requireActiveLanguageAndFramework(CreateSchoolRubricCommand command) {
        if (!languageRepository.existsById(command.languageId())) {
            throw new ResourceNotFoundException("The specified language does not exist in the system.");
        }
        Framework framework = frameworkRepository.findById(command.frameworkId())
                .orElseThrow(() -> new ResourceNotFoundException("Framework not found."));
        if (!framework.isActive()) {
            throw new IllegalArgumentException("This Framework has been disabled.");
        }
    }

    /**
     * Rejects a duplicate CODE within the same scope (school + language + framework).
     *
     * <p>Removed 2026-08-14: the old rule "a school has only one rubric per language". That rule forced
     * grades 10/11/12 to share one rubric, so a grade could only be encoded as a VERSION NUMBER (grade 10 =
     * v1, grade 11 = v2, grade 12 = v3). Then {@code version} lost its meaning of "a revision" and there was
     * no way to say "next year grade 10's own criteria are revised".
     *
     * <p>Now every grade has its own rubric, told apart by code (e.g. ENG-K10, ENG-K11), and each rubric
     * numbers its versions from 1. Nothing below relied on the old uniqueness: assessment policies point
     * straight at rubricVersionId instead of looking up a rubric by school + language, and the SYSTEM side
     * never had this rule (CreateSystemRubricUseCase). The real constraint is the unique index
     * (owner_type, school_id, language_id, framework_id, code); it is checked here to report a proper
     * error instead of letting the database throw a raw constraint violation.
     */
    private void requireCodeNotTaken(CreateSchoolRubricCommand command, String safeCode) {
        boolean isCodeTaken = rubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageIdAndFrameworkIdAndCode(
                RubricOwnerType.SCHOOL.toString(), command.schoolId(), command.languageId(),
                command.frameworkId(), safeCode);
        if (isCodeTaken) {
            throw new ResourceDuplicatedException("Your school already has an assessment Rubric with code '"
                    + safeCode + "' for this language and framework. Use another code"
                    + " (for example named after the grade level: ENG-K10, ENG-K11).");
        }
    }
}