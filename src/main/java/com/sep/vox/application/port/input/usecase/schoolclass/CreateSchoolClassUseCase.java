package com.sep.vox.application.port.input.usecase.schoolclass;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.schoolclass.CreateSchoolClassResponseMapper;
import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
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
public class CreateSchoolClassUseCase implements IUseCase<CreateSchoolClassCommand, CreateSchoolClassResponse> {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public CreateSchoolClassUseCase(
            SchoolClassRepository schoolClassRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            SchoolGradeRepository schoolGradeRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public CreateSchoolClassResponse execute(CreateSchoolClassCommand input) {
        var command = normalize(input);
        var now = Instant.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);

        validateRequestedSchool(command.schoolId(), schoolId);
        validateSchool(schoolId);
        validateLanguage(command.languageId());
        validateSchoolGrade(command.schoolGradeId(), schoolId);

        validateClassCodeIsUnique(schoolId, command.code());

        var schoolClass = SchoolClass.create(
            schoolId,
            command.languageId(),
            command.schoolGradeId(),
            command.code(),
            command.name(),
            command.description(),
            currentUserId,
            now
        );
        var saved = schoolClassRepository.save(schoolClass);
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
        var user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
        validateCurrentUserIsActive(user);
        return user;
    }

    private void validateCurrentUserIsActive(User currentUser) {
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Người dùng hiện tại không hoạt động");
        }
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalStateException("Người dùng hiện tại không thuộc trường nào"));
        return schoolUser.getSchoolId();
    }

    private void validateSchool(UUID schoolId) {
        var school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học không hoạt động");
        }
    }

    private void validateLanguage(UUID languageId) {
        var language = supportedLanguageRepository.findById(languageId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngôn ngữ"));
        if (!language.isActive()) {
            throw new IllegalStateException("Ngôn ngữ không hoạt động");
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
        var grade = schoolGradeRepository.findById(schoolGradeId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khối học"));
        // Năm học đã lưu schoolId trực tiếp -- không còn phải bắc cầu qua Khối để xác định trường sở hữu.
        if (!Objects.equals(grade.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Khối học không thuộc trường hiện tại");
        }
        if (grade.getStatus() != SchoolGradeStatus.ACTIVE) {
            throw new IllegalStateException("Khối học không hoạt động");
        }
    }


    private void validateClassCodeIsUnique(UUID schoolId, String code) {
        if (schoolClassRepository.findBySchoolIdAndCode(schoolId, code).isPresent()) {
            throw new DuplicatedException("Mã lớp học đã tồn tại trong trường");
        }
    }
}
