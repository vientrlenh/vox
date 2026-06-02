package com.sep.vox.application.port.input.usecase.schoolclass;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ImportValidationException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ImportSchoolClassRowCommand;
import com.sep.vox.application.port.input.command.ImportSchoolClassesCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ImportRowErrorDto;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassImportResultDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.model.languagelevel.LevelStatus;
import com.sep.vox.domain.model.schoolclass.SchoolClass;
import com.sep.vox.domain.model.schoolgrade.SchoolGradeStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolLevelRepository;
import com.sep.vox.domain.repository.SchoolLevelVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ImportSchoolClassesUseCase implements IUseCase<ImportSchoolClassesCommand, SchoolClassImportResultDto> {

    private static final int CODE_MAX_LENGTH = 100;
    private static final int NAME_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MAX_LENGTH = 2048;

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolLevelRepository schoolLevelRepository;
    private final SchoolLevelVersionRepository schoolLevelVersionRepository;
    private final UserContextPort userContextPort;

    public ImportSchoolClassesUseCase(
            SchoolClassRepository schoolClassRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolLevelRepository schoolLevelRepository,
            SchoolLevelVersionRepository schoolLevelVersionRepository,
            UserContextPort userContextPort) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolLevelRepository = schoolLevelRepository;
        this.schoolLevelVersionRepository = schoolLevelVersionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SchoolClassImportResultDto execute(ImportSchoolClassesCommand input) {
        var rows = input == null || input.rows() == null ? List.<ImportSchoolClassRowCommand>of() : input.rows();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        if (rows.isEmpty()) {
            throw new ImportValidationException(List.of(new ImportRowErrorDto(0, "file", "File import không có dữ liệu")));
        }

        var errors = new ArrayList<ImportRowErrorDto>();
        var normalizedRows = rows.stream()
            .map(this::normalize)
            .toList();
        validateRequiredAndLength(normalizedRows, errors);
        validateDuplicateCodesInFile(normalizedRows, errors);
        validateDuplicateCodesInDatabase(schoolId, normalizedRows, errors);

        var candidates = new ArrayList<CreateCandidate>();
        for (var row : normalizedRows) {
            var candidate = resolveCandidate(row, schoolId, currentUserId, errors);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        if (!errors.isEmpty()) {
            throw new ImportValidationException(errors);
        }

        var createdClasses = new ArrayList<SchoolClassDto>();
        for (var candidate : candidates) {
            var saved = schoolClassRepository.save(candidate.schoolClass());
            createdClasses.add(SchoolClassDtoMapper.toDto(saved));
        }

        return new SchoolClassImportResultDto(rows.size(), createdClasses.size(), List.copyOf(createdClasses));
    }

    private NormalizedRow normalize(ImportSchoolClassRowCommand row) {
        return new NormalizedRow(
            row.rowNumber(),
            normalizeCode(row.languageCode()),
            normalizeCode(row.schoolGradeCode()),
            normalizeCode(row.targetSchoolLevelCode()),
            StringNormalization.trimAndCollapseSpaces(row.targetSchoolLevelVersion()),
            StringNormalization.normalizeClassCode(row.code()),
            StringNormalization.trimAndCollapseSpaces(row.name()),
            StringNormalization.trimAndCollapseSpaces(row.description())
        );
    }

    private void validateRequiredAndLength(List<NormalizedRow> rows, List<ImportRowErrorDto> errors) {
        for (var row : rows) {
            require(row, row.languageCode(), "languageCode", errors);
            require(row, row.schoolGradeCode(), "schoolGradeCode", errors);
            require(row, row.targetSchoolLevelCode(), "targetSchoolLevelCode", errors);
            require(row, row.targetSchoolLevelVersion(), "targetSchoolLevelVersion", errors);
            require(row, row.code(), "code", errors);
            require(row, row.name(), "name", errors);
            maxLength(row, row.code(), "code", CODE_MAX_LENGTH, errors);
            maxLength(row, row.name(), "name", NAME_MAX_LENGTH, errors);
            maxLength(row, row.description(), "description", DESCRIPTION_MAX_LENGTH, errors);
        }
    }

    private void validateDuplicateCodesInFile(List<NormalizedRow> rows, List<ImportRowErrorDto> errors) {
        var firstRowByCode = new HashMap<String, Integer>();
        for (var row : rows) {
            if (isBlank(row.code())) {
                continue;
            }
            var firstRow = firstRowByCode.putIfAbsent(row.code(), row.rowNumber());
            if (firstRow != null) {
                errors.add(new ImportRowErrorDto(row.rowNumber(), "code", "Mã lớp học bị trùng với dòng " + firstRow));
            }
        }
    }

    private void validateDuplicateCodesInDatabase(UUID schoolId, List<NormalizedRow> rows, List<ImportRowErrorDto> errors) {
        var codes = rows.stream()
            .map(NormalizedRow::code)
            .filter(code -> !isBlank(code))
            .collect(java.util.stream.Collectors.toSet());
        if (codes.isEmpty()) {
            return;
        }
        var existingCodes = schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, codes)
            .stream()
            .map(schoolClass -> schoolClass.getCode().value())
            .collect(java.util.stream.Collectors.toSet());
        for (var row : rows) {
            if (existingCodes.contains(row.code())) {
                errors.add(new ImportRowErrorDto(row.rowNumber(), "code", "Mã lớp học đã tồn tại trong trường"));
            }
        }
    }

    private CreateCandidate resolveCandidate(NormalizedRow row, UUID schoolId, UUID currentUserId,
            List<ImportRowErrorDto> errors) {
        if (hasStructuralError(row)) {
            return null;
        }

        var language = supportedLanguageRepository.findByCode(row.languageCode());
        if (language.isEmpty()) {
            errors.add(new ImportRowErrorDto(row.rowNumber(), "languageCode", "Không tìm thấy ngôn ngữ"));
            return null;
        }
        if (!language.get().isActive()) {
            errors.add(new ImportRowErrorDto(row.rowNumber(), "languageCode", "Ngôn ngữ không hoạt động"));
        }

        var grade = schoolGradeRepository.findBySchoolIdAndCode(schoolId, row.schoolGradeCode());
        if (grade.isEmpty()) {
            errors.add(new ImportRowErrorDto(row.rowNumber(), "schoolGradeCode", "Không tìm thấy khối học trong trường hiện tại"));
            return null;
        }
        if (grade.get().getStatus() != SchoolGradeStatus.ACTIVE) {
            errors.add(new ImportRowErrorDto(row.rowNumber(), "schoolGradeCode", "Khối học không hoạt động"));
        }

        var level = schoolLevelRepository.findBySchoolIdAndLanguageIdAndCode(
            schoolId,
            language.get().getId(),
            row.targetSchoolLevelCode()
        );
        if (level.isEmpty()) {
            errors.add(new ImportRowErrorDto(row.rowNumber(), "targetSchoolLevelCode", "Không tìm thấy cấp độ mục tiêu theo ngôn ngữ đã chọn"));
            return null;
        }

        var version = parseVersion(row, errors);
        if (version == null) {
            return null;
        }

        var levelVersion = schoolLevelVersionRepository.findBySchoolLevelIdAndVersion(level.get().getId(), version);
        if (levelVersion.isEmpty()) {
            errors.add(new ImportRowErrorDto(row.rowNumber(), "targetSchoolLevelVersion", "Không tìm thấy phiên bản cấp độ mục tiêu"));
            return null;
        }
        if (levelVersion.get().getStatus() != LevelStatus.PUBLISHED) {
            errors.add(new ImportRowErrorDto(row.rowNumber(), "targetSchoolLevelVersion", "Phiên bản cấp độ mục tiêu chưa được công bố"));
        }

        if (hasRowError(row.rowNumber(), errors)) {
            return null;
        }

        var now = OffsetDateTime.now();
        var schoolClass = SchoolClass.create(
            schoolId,
            language.get().getId(),
            grade.get().getId(),
            row.code(),
            row.name(),
            row.description(),
            levelVersion.get().getId(),
            currentUserId,
            now
        );
        return new CreateCandidate(schoolClass);
    }

    private Integer parseVersion(NormalizedRow row, List<ImportRowErrorDto> errors) {
        try {
            var version = Integer.parseInt(row.targetSchoolLevelVersion());
            if (version <= 0) {
                errors.add(new ImportRowErrorDto(row.rowNumber(), "targetSchoolLevelVersion", "Phiên bản cấp độ mục tiêu phải lớn hơn 0"));
                return null;
            }
            return version;
        } catch (NumberFormatException e) {
            errors.add(new ImportRowErrorDto(row.rowNumber(), "targetSchoolLevelVersion", "Phiên bản cấp độ mục tiêu phải là số nguyên"));
            return null;
        }
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

    private static void require(NormalizedRow row, String value, String field, List<ImportRowErrorDto> errors) {
        if (isBlank(value)) {
            errors.add(new ImportRowErrorDto(row.rowNumber(), field, "Trường bắt buộc không được để trống"));
        }
    }

    private static void maxLength(NormalizedRow row, String value, String field, int maxLength,
            List<ImportRowErrorDto> errors) {
        if (value != null && value.length() > maxLength) {
            errors.add(new ImportRowErrorDto(row.rowNumber(), field, "Độ dài không được vượt quá " + maxLength + " ký tự"));
        }
    }

    private static boolean hasStructuralError(NormalizedRow row) {
        return isBlank(row.languageCode())
            || isBlank(row.schoolGradeCode())
            || isBlank(row.targetSchoolLevelCode())
            || isBlank(row.targetSchoolLevelVersion())
            || isBlank(row.code())
            || isBlank(row.name())
            || row.code().length() > CODE_MAX_LENGTH
            || row.name().length() > NAME_MAX_LENGTH
            || (row.description() != null && row.description().length() > DESCRIPTION_MAX_LENGTH);
    }

    private static boolean hasRowError(int rowNumber, List<ImportRowErrorDto> errors) {
        return errors.stream().anyMatch(error -> error.rowNumber() == rowNumber);
    }

    private static String normalizeCode(String input) {
        if (input == null) {
            return null;
        }
        return input.strip().toUpperCase(java.util.Locale.ROOT);
    }

    private static boolean isBlank(String input) {
        return input == null || input.isBlank();
    }

    private record NormalizedRow(
            int rowNumber,
            String languageCode,
            String schoolGradeCode,
            String targetSchoolLevelCode,
            String targetSchoolLevelVersion,
            String code,
            String name,
            String description) {
    }

    private record CreateCandidate(SchoolClass schoolClass) {
    }
}
