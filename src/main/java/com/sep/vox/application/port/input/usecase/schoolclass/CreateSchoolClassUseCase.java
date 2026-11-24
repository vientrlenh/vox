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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));
        validateCurrentUserIsActive(user);
        return user;
    }

    private void validateCurrentUserIsActive(User currentUser) {
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Người dùng hiện tại không hoạt động");
        }
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Người dùng hiện tại không thuộc trường nào"));
        return schoolUser.getSchoolId();
    }

    private void validateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalArgumentException("Trường học không hoạt động");
        }
    }

    private void validateLanguage(UUID languageId) {
        SupportedLanguage language = supportedLanguageRepository.findById(languageId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngôn ngữ"));
        if (!language.isActive()) {
            throw new IllegalArgumentException("Ngôn ngữ không hoạt động");
        }
    }

    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (requestedSchoolId == null) {
            throw new IllegalArgumentException("Trường học không được để trống");
        }
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("Trường học không khớp với người dùng hiện tại");
        }
    }

    private void validateSchoolGrade(UUID schoolGradeId, UUID schoolId) {
        SchoolGrade grade = schoolGradeRepository.findById(schoolGradeId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khối học"));
        // Năm học đã lưu schoolId trực tiếp -- không còn phải bắc cầu qua Khối để xác định trường sở hữu.
        if (!Objects.equals(grade.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Khối học không thuộc trường hiện tại");
        }
        if (grade.getStatus() != SchoolGradeStatus.ACTIVE) {
            throw new IllegalArgumentException("Khối học không hoạt động");
        }
    }


    private void validateClassCodeIsUnique(UUID schoolId, String code) {
        if (schoolClassRepository.findBySchoolIdAndCode(schoolId, code).isPresent()) {
            throw new ResourceDuplicatedException("Mã lớp học đã tồn tại trong trường");
        }
    }
}
