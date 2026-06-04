package com.sep.vox.application.port.input.usecase.schoolclass;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ImportSchoolClassRowCommand;
import com.sep.vox.application.port.input.command.ImportSchoolClassesCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassImportResultDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ImportSchoolClassesUseCase implements IUseCase<ImportSchoolClassesCommand, Void> {

    private static final int CODE_MAX_LENGTH = 100;
    private static final int NAME_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MAX_LENGTH = 2048;

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final UserContextPort userContextPort;

    public ImportSchoolClassesUseCase(
            SchoolClassRepository schoolClassRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            SchoolGradeRepository schoolGradeRepository,
            UserContextPort userContextPort) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(ImportSchoolClassesCommand input) {
        var rows = input == null || input.rows() == null ? List.<ImportSchoolClassRowCommand>of() : input.rows();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        if (rows.isEmpty()) {
            throw new IllegalStateException("File không chứa dữ liệu");
        }

        var normalizedRows = rows.stream()
            .map(this::normalize)
            .toList();
        validateRequiredAndLengthOrThrow(normalizedRows);
        validateDuplicateCodesInFileOrThrow(normalizedRows);
        validateDuplicateCodesInDatabaseOrThrow(schoolId, normalizedRows);

        var candidates = new ArrayList<CreateCandidate>();
        for (var row : normalizedRows) {
            candidates.add(resolveCandidateOrThrow(row, schoolId, currentUserId));
        }

        var createdClasses = new ArrayList<SchoolClassDto>();
        for (var candidate : candidates) {
            var saved = schoolClassRepository.save(candidate.schoolClass());
            createdClasses.add(SchoolClassDtoMapper.toDto(saved));
        }

        return null;
    }

    private NormalizedRow normalize(ImportSchoolClassRowCommand row) {
        return new NormalizedRow(
            row.rowNumber(),
            normalizeCode(row.languageCode()),
            normalizeCode(row.schoolGradeCode()),
            normalizeCode(row.targetSchoolLevelCode()),
            StringNormalization.trimAndCollapseSpaces(row.targetSchoolLevelVersion()),
            StringNormalization.normalizeCode(row.code()),
            StringNormalization.trimAndCollapseSpaces(row.name()),
            StringNormalization.trimAndCollapseSpaces(row.description())
        );
    }

    private void validateRequiredAndLengthOrThrow(List<NormalizedRow> rows) {
        for (var row : rows) {
            requireOrThrow(row, row.languageCode(), "languageCode");
            requireOrThrow(row, row.schoolGradeCode(), "schoolGradeCode");
            requireOrThrow(row, row.targetSchoolLevelCode(), "targetSchoolLevelCode");
            requireOrThrow(row, row.targetSchoolLevelVersion(), "targetSchoolLevelVersion");
            requireOrThrow(row, row.code(), "code");
            requireOrThrow(row, row.name(), "name");
            maxLengthOrThrow(row, row.code(), "code", CODE_MAX_LENGTH);
            maxLengthOrThrow(row, row.name(), "name", NAME_MAX_LENGTH);
            maxLengthOrThrow(row, row.description(), "description", DESCRIPTION_MAX_LENGTH);
        }
    }

    private void requireOrThrow(NormalizedRow row, String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Trường bắt buộc đang thiếu: " + field);
        }
    }

    private void maxLengthOrThrow(NormalizedRow row, String value, String field, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException("Độ dài trường " + field + " không được vượt quá " + maxLength + " ký tự");
        }
    }

    private void validateDuplicateCodesInFileOrThrow(List<NormalizedRow> rows) {
        var firstRowByCode = new HashMap<String, Integer>();
        for (var row : rows) {
            if (isBlank(row.code())) {
                continue;
            }
            var firstRow = firstRowByCode.putIfAbsent(row.code(), row.rowNumber());
            if (firstRow != null) {
                throw new IllegalArgumentException("Mã lớp học đã trùng với dòng " + firstRow);
            }
        }
    }

    private void validateDuplicateCodesInDatabaseOrThrow(UUID schoolId, List<NormalizedRow> rows) {
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
                throw new IllegalArgumentException("Mã lớp học đã tồn tại trong trường: " + row.code());
            }
        }
    }

    private CreateCandidate resolveCandidateOrThrow(NormalizedRow row, UUID schoolId, UUID currentUserId) {
        var language = supportedLanguageRepository.findByCode(row.languageCode());
        if (language.isEmpty()) {
            throw new NotFoundException("Không tìm thấy ngôn ngữ");
        }
        if (!language.get().isActive()) {
            throw new IllegalStateException("Ngôn ngữ không hoạt động");
        }

        var grade = schoolGradeRepository.findBySchoolIdAndCode(schoolId, row.schoolGradeCode()).orElseThrow(() -> new NotFoundException("Không tìm thấy khối học trong trường"));
        if (grade.get().getStatus() != SchoolGradeStatus.ACTIVE) {
            throw new IllegalStateException("Khối học không hoạt động");
        }

        var now = OffsetDateTime.now();
        var schoolClass = SchoolClass.create(
            schoolId, 
            currentUserId, 
            grade, null, null, null, currentUserId, now)
        return new CreateCandidate(schoolClass);
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
