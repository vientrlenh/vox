package com.sep.vox.infrastructure.adapter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.common.importer.ImportFileFormat;
import com.sep.vox.application.common.importer.ImportRow;
import com.sep.vox.application.common.importer.JsonPathResolver;
import com.sep.vox.application.port.input.command.ImportFieldMapping;
import com.sep.vox.application.response.input.schooluser.SchoolUserImportError;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.UserRepository;

@Component
public class SchoolUserImportValidator {

    private static final List<String> ALLOWED_ROLE_CODES = List.of("STUDENT", "TEACHER");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public SchoolUserImportValidator(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public RowValidationResult validateAndPrepareRow(
            ImportRow row,
            ImportFileFormat format,
            Map<String, ImportFieldMapping> mapping,
            String defaultRole,
            Set<String> seenEmails,
            Set<String> seenPhones,
            boolean checkDatabaseConstraints) {
        var rowErrors = new ArrayList<SchoolUserImportError>();

        var emailRaw = resolveField(row, format, mapping.get("email"), "email");
        var phoneRaw = resolveField(row, format, mapping.get("phone"), "phone");
        var fullNameRaw = resolveField(row, format, mapping.get("fullName"), "fullName");
        var dobRaw = resolveField(row, format, mapping.get("dateOfBirth"), "dateOfBirth");
        var roleRaw = resolveField(row, format, mapping.get("roleCode"), "roleCode");
        var addressRaw = resolveField(row, format, mapping.get("address"), "address");
        var studentIdRaw = resolveField(row, format, mapping.get("studentId"), "studentId");

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

        var resolvedRoleRaw = roleRaw != null ? roleRaw.trim() : null;
        var defaultRoleCode = defaultRole != null ? defaultRole.trim() : null;
        if (isBlank(resolvedRoleRaw)) {
            resolvedRoleRaw = defaultRoleCode;
        }
        if (isBlank(resolvedRoleRaw)) {
            rowErrors.add(error(row.rowNumber(), "roleCode", "REQUIRED", "Vai trò không được để trống", roleRaw));
        }

        var email = emailRaw != null ? StringNormalization.normalizeEmail(emailRaw) : null;
        var phone = phoneRaw != null ? StringNormalization.normalizePhone(phoneRaw) : null;
        var fullName = fullNameRaw != null ? StringNormalization.trimAndCollapseSpaces(fullNameRaw) : null;
        var address = addressRaw != null ? StringNormalization.trimAndCollapseSpaces(addressRaw) : null;
        var studentId = studentIdRaw != null ? studentIdRaw.trim() : null;
        var roleCode = resolvedRoleRaw != null ? resolvedRoleRaw.trim().toUpperCase(Locale.ROOT) : "";

        var dateOfBirth = parseDateOfBirth(dobRaw, mapping.get("dateOfBirth"));

        if (dateOfBirth == null && !isBlank(dobRaw)) {
            rowErrors.add(error(row.rowNumber(), "dateOfBirth", "INVALID_FORMAT", "Định dạng ngày không hợp lệ", dobRaw));
        }
        if (!ALLOWED_ROLE_CODES.contains(roleCode)) {
            rowErrors.add(error(row.rowNumber(), "roleCode", "INVALID_VALUE", "Vai trò không hợp lệ", roleCode));
        }
        if (email != null && !seenEmails.add(email)) {
            rowErrors.add(error(row.rowNumber(), "email", "DUPLICATE", "Email bị trùng trong file", emailRaw));
        }
        if (phone != null && !seenPhones.add(phone)) {
            rowErrors.add(error(row.rowNumber(), "phone", "DUPLICATE", "Số điện thoại bị trùng trong file", phoneRaw));
        }

        if (checkDatabaseConstraints) {
            if (email != null && userRepository.findByEmail(email).isPresent()) {
                rowErrors.add(error(row.rowNumber(), "email", "DUPLICATE", "Email đã tồn tại", emailRaw));
            }
            if (phone != null && userRepository.findByPhone(phone).isPresent()) {
                rowErrors.add(error(row.rowNumber(), "phone", "DUPLICATE", "Số điện thoại đã tồn tại", phoneRaw));
            }
            if (roleRepository.findByCode(roleCode).isEmpty()) {
                rowErrors.add(error(row.rowNumber(), "roleCode", "NOT_FOUND", "Không tìm thấy vai trò", roleCode));
            }
        }

        var payload = mappedPayload(email, phone, fullName, dobRaw, roleCode, address, studentId);
        if (!rowErrors.isEmpty()) {
            return RowValidationResult.invalid(rowErrors, payload);
        }
        return RowValidationResult.valid(email, phone, fullName, dateOfBirth, address, studentId, roleCode, payload);
    }

    private static Map<String, Object> mappedPayload(
            String email,
            String phone,
            String fullName,
            String dateOfBirth,
            String roleCode,
            String address,
            String studentId) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("email", email);
        payload.put("phone", phone);
        payload.put("fullName", fullName);
        payload.put("dateOfBirth", dateOfBirth);
        payload.put("roleCode", roleCode);
        payload.put("address", address);
        payload.put("studentId", studentId);
        return payload;
    }

    private static String resolveField(ImportRow row, ImportFileFormat format, ImportFieldMapping mapping, String semanticName) {
        if (format == ImportFileFormat.JSON) {
            if (mapping != null && mapping.path() != null && row.jsonValues() != null) {
                var resolved = JsonPathResolver.resolve(row.jsonValues(), mapping.path());
                return resolved != null ? resolved.toString() : null;
            }
            var resolved = resolveByCandidates(row.jsonValues(), mapping, semanticName);
            if (resolved != null) {
                return resolved;
            }
            return null;
        }
        var resolved = resolveByCandidates(row.columns(), mapping, semanticName);
        if (resolved != null) {
            return resolved;
        }
        if (mapping != null && mapping.index() != null && row.values() != null) {
            int index = mapping.index();
            if (index >= 0 && index < row.values().size()) {
                return row.values().get(index);
            }
        }
        return null;
    }

    private static String resolveByCandidates(Map<String, ?> values, ImportFieldMapping mapping, String semanticName) {
        if (values == null) {
            return null;
        }
        for (var candidate : candidateNames(mapping, semanticName)) {
            var direct = values.get(candidate);
            if (direct != null) {
                return direct.toString();
            }
        }
        for (var entry : values.entrySet()) {
            var key = entry.getKey();
            var normalizedKey = normalizeHeader(key != null ? key.toString() : null);
            if (normalizedKey == null) {
                continue;
            }
            for (var candidate : candidateNames(mapping, semanticName)) {
                if (normalizedKey.equals(normalizeHeader(candidate))) {
                    var value = entry.getValue();
                    return value != null ? value.toString() : null;
                }
            }
        }
        return null;
    }

    private static List<String> candidateNames(ImportFieldMapping mapping, String semanticName) {
        var candidates = new LinkedHashSet<String>();
        if (mapping != null) {
            if (mapping.column() != null && !mapping.column().isBlank()) {
                candidates.add(mapping.column());
            }
            if (mapping.aliases() != null) {
                for (var alias : mapping.aliases()) {
                    if (alias != null && !alias.isBlank()) {
                        candidates.add(alias);
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            switch (semanticName) {
                case "email" -> candidates.addAll(List.of("Email", "E-mail", "E mail", "email", "Địa chỉ mail", "Địa chỉ email"));
                case "phone" -> candidates.addAll(List.of("Phone", "Phone Number", "Số điện thoại", "Điện thoại", "sđt", "Số điện thoại liên lạc"));
                case "fullName" -> candidates.addAll(List.of("Full Name", "Họ và tên", "Tên", "Name", "fullName", "Họ tên"));
                case "dateOfBirth" -> candidates.addAll(List.of("DOB", "Date of Birth", "Ngày sinh", "Birth Date", "dateOfBirth"));
                case "roleCode" -> candidates.addAll(List.of("Role", "Vai trò", "role", "Chức vụ"));
                case "address" -> candidates.addAll(List.of("Address", "Địa chỉ", "address", "Nơi ở", "Nơi ở hiện tại", "Địa chỉ liên lạc"));
                case "studentId" -> candidates.addAll(List.of("Student ID", "Mã học sinh", "studentId", "mã HS"));
                default -> candidates.add(semanticName);
            }
        }
        return List.copyOf(candidates);
    }

    private static String normalizeHeader(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return StringNormalization.normalizeSearchText(value).toLowerCase(Locale.ROOT);
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

    private static SchoolUserImportError error(long rowNumber, String field, String code, String message, String rawValue) {
        return new SchoolUserImportError((int) rowNumber, field, code, message, rawValue);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record RowValidationResult(
        String email,
        String phone,
        String fullName,
        LocalDate dateOfBirth,
        String address,
        String studentId,
        String roleCode,
        List<SchoolUserImportError> errors,
        Map<String, Object> mappedPayload
    ) {
        public static RowValidationResult valid(
                String email,
                String phone,
                String fullName,
                LocalDate dateOfBirth,
                String address,
                String studentId,
                String roleCode,
                Map<String, Object> payload) {
            return new RowValidationResult(email, phone, fullName, dateOfBirth, address, studentId, roleCode, List.of(), payload);
        }

        public static RowValidationResult invalid(List<SchoolUserImportError> errors, Map<String, Object> payload) {
            return new RowValidationResult(null, null, null, null, null, null, null, List.copyOf(errors), payload);
        }
    }
}
