package com.sep.vox.application.port.input.usecase.schoolclass;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.schoolclass.CreateSchoolClassResponseMapper;
import com.sep.vox.application.port.input.command.schoolclass.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CreateSchoolClassUseCase implements IUseCase<CreateSchoolClassCommand, CreateSchoolClassResponse> {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public CreateSchoolClassResponse execute(CreateSchoolClassCommand input) {
        CreateSchoolClassCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        UUID schoolId = getSchoolId(currentUser);

        validateRequestedSchool(command.schoolId(), schoolId);
        validateSchool(schoolId);
        validateLanguage(command.languageId());
        validateSchoolGrade(command.schoolGradeId(), schoolId);

        validateClassCodeIsUnique(schoolId, command.code());

        SchoolClass schoolClass = SchoolClass.create(
            schoolId,
            command.languageId(),
            command.schoolGradeId(),
            command.code(),
            command.name(),
            command.description()
        );
        SchoolClass saved = schoolClassRepository.save(schoolClass);
        return CreateSchoolClassResponseMapper.toResponse(saved.getId());
    }

    private CreateSchoolClassCommand normalize(CreateSchoolClassCommand input) {
        return new CreateSchoolClassCommand(
            input.schoolId(),
            input.languageId(),
            input.schoolGradeId(),
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }

    private User findCurrentUser(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        validateCurrentUserIsActive(user);
        return user;
    }

    private void validateCurrentUserIsActive(User currentUser) {
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("The current user is not active");
        }
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("The current user does not belong to any school"));
        return schoolUser.getSchoolId();
    }

    private void validateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        if (!school.isActive()) {
            throw new IllegalArgumentException("The school is not active");
        }
    }

    private void validateLanguage(UUID languageId) {
        SupportedLanguage language = supportedLanguageRepository.findById(languageId)
            .orElseThrow(() -> new ResourceNotFoundException("Language not found"));
        if (!language.isActive()) {
            throw new IllegalArgumentException("The language is not active");
        }
    }

    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (requestedSchoolId == null) {
            throw new IllegalArgumentException("The school must not be empty");
        }
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("The school does not match the current user");
        }
    }

    private void validateSchoolGrade(UUID schoolGradeId, UUID schoolId) {
        SchoolGrade grade = schoolGradeRepository.findById(schoolGradeId)
            .orElseThrow(() -> new ResourceNotFoundException("Grade level not found"));
        // The school year stores schoolId directly -- no need to go through the Grade to find the owning school.
        if (!Objects.equals(grade.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("The grade level does not belong to the current school");
        }
        if (grade.getStatus() != SchoolGradeStatus.ACTIVE) {
            throw new IllegalArgumentException("The grade level is not active");
        }
    }


    private void validateClassCodeIsUnique(UUID schoolId, String code) {
        if (schoolClassRepository.findBySchoolIdAndCode(schoolId, code).isPresent()) {
            throw new ResourceDuplicatedException("The class code already exists in the school");
        }
    }
}
