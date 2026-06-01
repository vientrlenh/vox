package com.sep.vox.application.port.input.usecase.schooladmin;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.model.languagelevel.LevelStatus;
import com.sep.vox.domain.model.schoolclass.SchoolClass;
import com.sep.vox.domain.model.schoolgrade.SchoolGradeStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolLevelRepository;
import com.sep.vox.domain.repository.SchoolLevelVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class CreateSchoolClassUseCase implements IUseCase<CreateSchoolClassCommand, SchoolClassDto> {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolLevelVersionRepository schoolLevelVersionRepository;
    private final SchoolLevelRepository schoolLevelRepository;
    private final UserContextPort userContextPort;

    public CreateSchoolClassUseCase(
            SchoolClassRepository schoolClassRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolLevelVersionRepository schoolLevelVersionRepository,
            SchoolLevelRepository schoolLevelRepository,
            UserContextPort userContextPort) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolLevelVersionRepository = schoolLevelVersionRepository;
        this.schoolLevelRepository = schoolLevelRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SchoolClassDto execute(CreateSchoolClassCommand input) {
        var command = normalize(input);
        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);

        validateSchool(schoolId);
        validateLanguage(command.languageId());
        validateSchoolGrade(command.schoolGradeId(), schoolId);
        validateTargetSchoolLevelVersion(command.targetSchoolLevelVersionId(), schoolId, command.languageId());
        validateClassCodeIsUnique(schoolId, command.code());

        var schoolClass = SchoolClass.create(
            schoolId,
            command.languageId(),
            command.schoolGradeId(),
            command.code(),
            command.name(),
            command.description(),
            command.targetSchoolLevelVersionId(),
            currentUserId,
            now
        );
        var saved = schoolClassRepository.save(schoolClass);
        return SchoolClassDtoMapper.toDto(saved);
    }

    private CreateSchoolClassCommand normalize(CreateSchoolClassCommand input) {
        return new CreateSchoolClassCommand(
            input.languageId(),
            input.schoolGradeId(),
            StringNormalization.normalizeClassCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.targetSchoolLevelVersionId()
        );
    }

    private User findCurrentUser(UUID currentUserId) {
        return userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
    }

    private UUID getSchoolId(User currentUser) {
        var schoolId = currentUser.getSchoolId();
        if (schoolId == null) {
            throw new IllegalStateException("Người dùng hiện tại không thuộc trường nào");
        }
        return schoolId;
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

    private void validateSchoolGrade(UUID schoolGradeId, UUID schoolId) {
        var grade = schoolGradeRepository.findById(schoolGradeId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khối học"));
        if (!Objects.equals(grade.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Khối học không thuộc trường hiện tại");
        }
        if (grade.getStatus() != SchoolGradeStatus.ACTIVE) {
            throw new IllegalStateException("Khối học không hoạt động");
        }
    }

    private void validateTargetSchoolLevelVersion(UUID targetSchoolLevelVersionId, UUID schoolId, UUID languageId) {
        var levelVersion = schoolLevelVersionRepository.findById(targetSchoolLevelVersionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản cấp độ mục tiêu"));
        if (levelVersion.getStatus() != LevelStatus.PUBLISHED) {
            throw new IllegalStateException("Phiên bản cấp độ mục tiêu chưa được công bố");
        }

        var schoolLevel = schoolLevelRepository.findById(levelVersion.getSchoolLevelId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy cấp độ trường học"));
        if (!Objects.equals(schoolLevel.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Cấp độ mục tiêu không thuộc trường hiện tại");
        }
        if (!Objects.equals(schoolLevel.getLanguageId(), languageId)) {
            throw new IllegalArgumentException("Cấp độ mục tiêu không thuộc ngôn ngữ đã chọn");
        }
    }

    private void validateClassCodeIsUnique(UUID schoolId, String code) {
        if (schoolClassRepository.findBySchoolIdAndCode(schoolId, code).isPresent()) {
            throw new DuplicatedException("Mã lớp học đã tồn tại trong trường");
        }
    }
}
