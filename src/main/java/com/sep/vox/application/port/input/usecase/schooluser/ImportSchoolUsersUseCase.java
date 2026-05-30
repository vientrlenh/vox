package com.sep.vox.application.port.input.usecase.schooluser;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.common.importer.ImportFileFormat;
import com.sep.vox.application.common.importer.ImportParserFactory;
import com.sep.vox.application.common.importer.ImportRow;
import com.sep.vox.application.common.importer.JsonPathResolver;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ImportFieldMapping;
import com.sep.vox.application.port.input.command.ImportSchoolUsersCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.SchoolUserImportFileStoragePort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schooluser.SchoolUserImportError;
import com.sep.vox.application.response.input.schooluser.SchoolUserImportResponse;
import com.sep.vox.domain.model.schooluser.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.userrole.UserRole;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

@Service
public class ImportSchoolUsersUseCase implements IUseCase<ImportSchoolUsersCommand, SchoolUserImportResponse> {

    private static final List<String> ALLOWED_ROLE_CODES = List.of("STUDENT", "TEACHER");

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolUserImportFileStoragePort fileStoragePort;
    private final TransactionTemplate transactionTemplate;

    public ImportSchoolUsersUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolUserImportFileStoragePort fileStoragePort,
            PlatformTransactionManager transactionManager) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.fileStoragePort = fileStoragePort;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public SchoolUserImportResponse execute(ImportSchoolUsersCommand input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();
        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!input.schoolId().equals(caller.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var resource = fileStoragePort.load(input.fileId());
        try {
            var format = ImportFileFormat.valueOf(resource.format());
            var parser = ImportParserFactory.forFormat(format);

            List<ImportRow> rows;
            try (var inputStream = resource.inputStream()) {
                rows = parser.parse(inputStream);
            } catch (Exception e) {
            throw new IllegalArgumentException("Không thể đọc dữ liệu import", e);
            }

        var errors = new ArrayList<SchoolUserImportError>();
        var createdUserIds = new ArrayList<UUID>();
        var seenEmails = new HashSet<String>();
        var seenPhones = new HashSet<String>();

        int failedCount = 0;
        int createdCount = 0;

        var mapping = input.mapping() != null ? input.mapping() : Map.<String, ImportFieldMapping>of();

        for (var row : rows) {
            var rowErrors = new ArrayList<SchoolUserImportError>();

            var emailRaw = resolveField(row, format, mapping.get("email"));
            var phoneRaw = resolveField(row, format, mapping.get("phone"));
            var fullNameRaw = resolveField(row, format, mapping.get("fullName"));
            var dobRaw = resolveField(row, format, mapping.get("dateOfBirth"));
            var roleRaw = resolveField(row, format, mapping.get("roleCode"));
            var addressRaw = resolveField(row, format, mapping.get("address"));
            var studentIdRaw = resolveField(row, format, mapping.get("studentId"));

            if (isBlank(emailRaw)) {
                rowErrors.add(error(row.rowNumber(), "email", "REQUIRED", "Email không được để trống", emailRaw));
            }
            if (isBlank(phoneRaw)) {
                rowErrors.add(error(row.rowNumber(), "phone", "REQUIRED", "Số điện thoại không được để trống", phoneRaw));
            }
            if (isBlank(fullNameRaw)) {
                rowErrors.add(error(row.rowNumber(), "fullName", "REQUIRED", "Họ và tên không được để trống", fullNameRaw));
            }
            if (isBlank(dobRaw)) {
                rowErrors.add(error(row.rowNumber(), "dateOfBirth", "REQUIRED", "Ngày sinh không được để trống", dobRaw));
            }

            var defaultRole = input.defaultRole() != null ? input.defaultRole().trim() : null;
            var roleCode = roleRaw != null ? roleRaw.trim() : null;
            if (isBlank(roleCode)) {
                roleCode = defaultRole;
            }
            if (isBlank(roleCode)) {
                rowErrors.add(error(row.rowNumber(), "roleCode", "REQUIRED", "Vai trò không được để trống", roleRaw));
            }

            if (!rowErrors.isEmpty()) {
                errors.addAll(rowErrors);
                failedCount++;
                continue;
            }

            if (roleCode == null) {
                errors.add(error(row.rowNumber(), "roleCode", "REQUIRED", "Vai trò không được để trống", roleRaw));
                failedCount++;
                continue;
            }

            var email = StringNormalization.normalizeEmail(emailRaw);
            var phone = StringNormalization.normalizePhone(phoneRaw);
            var fullName = StringNormalization.trimAndCollapseSpaces(fullNameRaw);
            var address = addressRaw != null ? StringNormalization.trimAndCollapseSpaces(addressRaw) : null;
            var studentId = studentIdRaw != null ? studentIdRaw.trim() : null;
            var resolvedRoleCode = roleCode.trim().toUpperCase(Locale.ROOT);

            var dateOfBirth = parseDateOfBirth(dobRaw, mapping.get("dateOfBirth"));
            if (dateOfBirth == null) {
                errors.add(error(row.rowNumber(), "dateOfBirth", "INVALID_FORMAT", "Định dạng ngày không hợp lệ", dobRaw));
                failedCount++;
                continue;
            }

            if (!ALLOWED_ROLE_CODES.contains(resolvedRoleCode)) {
                errors.add(error(row.rowNumber(), "roleCode", "INVALID_VALUE", "Vai trò không hợp lệ", resolvedRoleCode));
                failedCount++;
                continue;
            }

            if (!seenEmails.add(email)) {
                errors.add(error(row.rowNumber(), "email", "DUPLICATE", "Email bị trùng trong file", emailRaw));
                failedCount++;
                continue;
            }
            if (!seenPhones.add(phone)) {
                errors.add(error(row.rowNumber(), "phone", "DUPLICATE", "Số điện thoại bị trùng trong file", phoneRaw));
                failedCount++;
                continue;
            }

            if (userRepository.findByEmail(email).isPresent()) {
                errors.add(error(row.rowNumber(), "email", "DUPLICATE", "Email đã tồn tại", emailRaw));
                failedCount++;
                continue;
            }
            if (userRepository.findByPhone(phone).isPresent()) {
                errors.add(error(row.rowNumber(), "phone", "DUPLICATE", "Số điện thoại đã tồn tại", phoneRaw));
                failedCount++;
                continue;
            }

            var role = roleRepository.findByCode(resolvedRoleCode).orElse(null);
            if (role == null) {
                errors.add(error(row.rowNumber(), "roleCode", "NOT_FOUND", "Không tìm thấy vai trò", resolvedRoleCode));
                failedCount++;
                continue;
            }

            if (input.dryRun()) {
                continue;
            }

            var createdId = transactionTemplate.execute(status -> {
                var now = OffsetDateTime.now();
                User user = resolvedRoleCode.equals("STUDENT")
                    ? User.createStudent(email, phone, fullName, dateOfBirth, address, callerId, input.schoolId(), now)
                    : User.createTeacher(email, phone, fullName, dateOfBirth, address, callerId, input.schoolId(), now);

                var savedUser = userRepository.save(user);
                userRoleRepository.save(new UserRole(savedUser.getId(), role.getId(), now));
                if ("STUDENT".equals(resolvedRoleCode) && studentId != null) {
                    schoolUserRepository.save(SchoolUser.create(savedUser.getId(), input.schoolId(), studentId, callerId, now));
                }
                return savedUser.getId();
            });

            if (createdId != null) {
                createdUserIds.add(createdId);
                createdCount++;
            }
        }

        var totalRows = rows.size();
        var processedRows = totalRows;
        var skippedCount = totalRows - createdCount - failedCount;

            return new SchoolUserImportResponse(
                input.fileId(),
                input.dryRun(),
                totalRows,
                processedRows,
                createdCount,
                failedCount,
                skippedCount,
                errors,
                createdUserIds
            );
        } finally {
            fileStoragePort.delete(input.fileId());
        }
    }

    private static SchoolUserImportError error(int rowNumber, String field, String code, String message, String rawValue) {
        return new SchoolUserImportError(rowNumber, field, code, message, rawValue);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String resolveField(ImportRow row, ImportFileFormat format, ImportFieldMapping mapping) {
        if (mapping == null) {
            return null;
        }
        if (format == ImportFileFormat.JSON) {
            if (mapping.path() != null && row.jsonValues() != null) {
                var resolved = JsonPathResolver.resolve(row.jsonValues(), mapping.path());
                return resolved != null ? resolved.toString() : null;
            }
            if (mapping.column() != null && row.jsonValues() != null) {
                var resolved = row.jsonValues().get(mapping.column());
                return resolved != null ? resolved.toString() : null;
            }
            return null;
        }
        if (mapping.column() != null && row.columns() != null) {
            var value = findColumnValue(row.columns(), mapping.column());
            if (value != null) {
                return value;
            }
        }
        if (mapping.index() != null && row.values() != null) {
            int index = mapping.index();
            if (index >= 0 && index < row.values().size()) {
                return row.values().get(index);
            }
        }
        return null;
    }

    private static String findColumnValue(Map<String, String> columns, String columnName) {
        if (columns == null || columnName == null) {
            return null;
        }
        var direct = columns.get(columnName);
        if (direct != null) {
            return direct;
        }
        var normalized = columnName.trim().toLowerCase(Locale.ROOT);
        for (var entry : columns.entrySet()) {
            var key = entry.getKey();
            if (key != null && key.trim().toLowerCase(Locale.ROOT).equals(normalized)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static LocalDate parseDateOfBirth(String rawValue, ImportFieldMapping mapping) {
        if (rawValue == null) {
            return null;
        }
        var customFormat = mapping != null ? mapping.dateFormat() : null;
        if (customFormat == null || customFormat.isBlank()) {
            try {
                return DateMapper.toLocalDate(rawValue.strip());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        try {
            var formatter = DateTimeFormatter.ofPattern(customFormat.strip());
            return LocalDate.parse(rawValue.strip(), formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
