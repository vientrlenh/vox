package com.sep.vox.application.port.input.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.SchoolUserPasswordSetUpEmailRequestedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

@Service
public class SchoolUserImportCommitHandler implements ImportCommitHandler {

    private static final Set<String> SUPPORTED_FIELDS = Set.of("email", "fullName", "roleCode", "phone",
            "dateOfBirth", "startDate", "endDate", "address");

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordSetUpTokenPort passwordSetUpTokenPort;
    private final PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private final EventPublisherPort eventPublisherPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final TransactionTemplate transactionTemplate;

    public SchoolUserImportCommitHandler(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolRepository schoolRepository,
            PasswordSetUpTokenPort passwordSetUpTokenPort,
            PasswordSetUpTokenRepository passwordSetUpTokenRepository,
            EventPublisherPort eventPublisherPort,
            JsonSerializationPort jsonSerializationPort,
            PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolRepository = schoolRepository;
        this.passwordSetUpTokenPort = passwordSetUpTokenPort;
        this.passwordSetUpTokenRepository = passwordSetUpTokenRepository;
        this.eventPublisherPort = eventPublisherPort;
        this.jsonSerializationPort = jsonSerializationPort;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public ImportType supportedType() {
        return ImportType.USER;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        var schoolId = session.getSchoolId();
        var currentUserId = session.getCreatedBy();
        var mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        var school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        return processRows(rows, mapping, schoolId, school.getName(), currentUserId);
    }

    private ImportCommitResult processRows(List<ImportRow> rows, Map<String, String> confirmedMapping,
            UUID schoolId, String schoolName, UUID currentUserId) {
        var createdRows = 0L;
        var updatedRows = 0L;
        var invalidRows = 0L;
        var seenEmails = new HashSet<String>();
        var rowContexts = new ArrayList<RowContext>();
        var emails = new HashSet<String>();
        var roleCodes = new HashSet<String>();

        for (var row : rows) {
            var rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            var mappedData = mapRawData(rawData, confirmedMapping);
            var normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            rowContexts.add(new RowContext(row, normalized));
            addIfPresent(emails, normalized.get("email"));
            addIfPresent(roleCodes, normalized.get("roleCode"));
        }

        var existingUsersByEmail = findUsersByEmail(emails);
        var rolesByCode = findRolesByCode(roleCodes);
        var schoolUsersByUserId = findSchoolUsersByUserId(existingUsersByEmail.values(), schoolId);

        for (var rowContext : rowContexts) {
            var row = rowContext.row();
            var normalized = rowContext.normalized();
            var errors = validateRow(normalized, seenEmails, rolesByCode);

            if (!errors.isEmpty()) {
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                row.setStatus(ImportRowStatus.INVALID);
                invalidRows++;
                continue;
            }

            var email = normalized.get("email");
            var roleCode = normalized.get("roleCode");
            var existingUser = existingUsersByEmail.get(email);
            var role = rolesByCode.get(roleCode);

            try {
                if (existingUser != null) {
                    transactionTemplate.executeWithoutResult(status ->
                            updateUser(existingUser, normalized, schoolId, schoolUsersByUserId, roleCode, currentUserId));
                    updatedRows++;
                } else {
                    transactionTemplate.executeWithoutResult(status ->
                            createUser(normalized, role, schoolId, schoolName, currentUserId));
                    createdRows++;
                }
            } catch (DataIntegrityViolationException | IllegalArgumentException e) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("email", "Email hoặc số điện thoại đã tồn tại hoặc dữ liệu không hợp lệ"))));
                row.setStatus(ImportRowStatus.FAILED);
                invalidRows++;
                continue;
            }

            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
        }

        return new ImportCommitResult(createdRows, updatedRows, 0L, invalidRows);
    }

    private UUID createUser(Map<String, String> data, Role role, UUID schoolId, String schoolName, UUID currentUserId) {
        var ts = OffsetDateTime.now();
        var user = User.create(data.get("email"), data.get("phone"), data.get("fullName"),
                parseDate(data.get("dateOfBirth")), data.get("address"), null, currentUserId, ts);
        var savedUser = userRepository.save(user);
        userRoleRepository.save(new UserRole(savedUser.getId(), role.getId(), ts));
        if (SchoolRoleCodes.STUDENT.equals(role.getCode().value())) {
            var startOffset = parseDate(data.get("startDate")).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            var endOffset = parseDate(data.get("endDate")).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            schoolUserRepository.save(SchoolUser.create(savedUser.getId(), schoolId, startOffset, endOffset));
        } else if (SchoolRoleCodes.TEACHER.equals(role.getCode().value())) {
            schoolUserRepository.save(SchoolUser.create(savedUser.getId(), schoolId, ts, null));
        }
        var generatedToken = passwordSetUpTokenPort.generateToken();
        passwordSetUpTokenRepository.save(PasswordSetUpToken.create(savedUser.getId(), generatedToken.hashedToken()));
        eventPublisherPort.publish(new SchoolUserPasswordSetUpEmailRequestedEvent(
                data.get("email"), data.get("fullName"), schoolName, savedUser.getId(), generatedToken.rawToken()));
        return savedUser.getId();
    }

    private UUID updateUser(User existing, Map<String, String> data, UUID schoolId,
            Map<UUID, SchoolUser> schoolUsersByUserId, String roleCode, UUID currentUserId) {
        var ts = OffsetDateTime.now();
        existing.setFullName(new FullName(data.get("fullName")));
        existing.setPhone(new Phone(data.get("phone")));
        existing.setDateOfBirth(new DateOfBirth(parseDate(data.get("dateOfBirth"))));
        existing.setAddress(data.get("address"));
        existing.setUpdatedAt(ts);
        existing.setUpdatedBy(currentUserId);
        userRepository.save(existing);
        if (SchoolRoleCodes.STUDENT.equals(roleCode)) {
            var startOffset = parseDate(data.get("startDate")).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            var endOffset = parseDate(data.get("endDate")).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            var schoolUser = schoolUsersByUserId.get(existing.getId());
            if (schoolUser != null) {
                schoolUser.setStartDate(startOffset);
                schoolUser.setEndDate(endOffset);
                schoolUserRepository.save(schoolUser);
            } else {
                schoolUserRepository.save(SchoolUser.create(existing.getId(), schoolId, startOffset, endOffset));
            }
        } else if (SchoolRoleCodes.TEACHER.equals(roleCode) && schoolUsersByUserId.get(existing.getId()) == null) {
            schoolUserRepository.save(SchoolUser.create(existing.getId(), schoolId, ts, null));
        }
        return existing.getId();
    }

    private Map<String, User> findUsersByEmail(Set<String> emails) {
        var map = new LinkedHashMap<String, User>();
        userRepository.findByEmailIn(emails)
                .forEach(user -> map.putIfAbsent(user.getEmail().value(), user));
        return map;
    }

    private Map<String, Role> findRolesByCode(Set<String> codes) {
        var map = new LinkedHashMap<String, Role>();
        roleRepository.findByCodeIn(codes)
                .forEach(role -> map.putIfAbsent(role.getCode().value(), role));
        return map;
    }

    private Map<UUID, SchoolUser> findSchoolUsersByUserId(Collection<User> users, UUID schoolId) {
        var userIds = new HashSet<UUID>();
        users.forEach(user -> userIds.add(user.getId()));
        var map = new LinkedHashMap<UUID, SchoolUser>();
        schoolUserRepository.findByUserIdIn(userIds).forEach(su -> {
            if (Objects.equals(su.getSchoolId(), schoolId)) {
                map.putIfAbsent(su.getUserId(), su);
            }
        });
        return map;
    }

    private Map<String, String> mapRawData(Map<String, String> rawData, Map<String, String> confirmedMapping) {
        var mappedData = new LinkedHashMap<String, String>();
        rawData.forEach((originalHeader, value) -> {
            var systemField = confirmedMapping.get(originalHeader);
            if (systemField != null) {
                systemField = systemField.strip();
            }
            if (SUPPORTED_FIELDS.contains(systemField)) {
                mappedData.put(systemField, value);
            }
        });
        return mappedData;
    }

    private Map<String, String> normalize(Map<String, String> mappedData) {
        var normalized = new LinkedHashMap<String, String>();
        normalized.put("email", StringNormalization.normalizeEmail(mappedData.get("email")));
        normalized.put("fullName", StringNormalization.trimAndCollapseSpaces(mappedData.get("fullName")));
        normalized.put("roleCode", StringNormalization.normalizeCode(mappedData.get("roleCode")));
        normalized.put("phone", StringNormalization.normalizePhone(mappedData.get("phone")));
        normalized.put("dateOfBirth", trimOrNull(mappedData.get("dateOfBirth")));
        normalized.put("startDate", trimOrNull(mappedData.get("startDate")));
        normalized.put("endDate", trimOrNull(mappedData.get("endDate")));
        normalized.put("address", StringNormalization.trimAndCollapseSpaces(mappedData.get("address")));
        return normalized;
    }

    private List<Map<String, String>> validateRow(Map<String, String> data, Set<String> seenEmails,
            Map<String, Role> rolesByCode) {
        var errors = new ArrayList<Map<String, String>>();

        addMissingError(errors, data, "email", "Email không được để trống");
        addMissingError(errors, data, "fullName", "Họ tên không được để trống");
        addMissingError(errors, data, "roleCode", "Vai trò không được để trống");
        addMissingError(errors, data, "phone", "Số điện thoại không được để trống");
        addMissingError(errors, data, "dateOfBirth", "Ngày sinh không được để trống");
        addMissingError(errors, data, "startDate", "Ngày bắt đầu không được để trống");
        addMissingError(errors, data, "endDate", "Ngày kết thúc không được để trống");
        addMissingError(errors, data, "address", "Địa chỉ không được để trống");

        var email = data.get("email");
        if (isPresent(email)) {
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                errors.add(error("email", "Email không hợp lệ"));
            } else if (!seenEmails.add(email)) {
                errors.add(error("email", "Email bị trùng trong file import"));
            }
        }

        var roleCode = data.get("roleCode");
        if (isPresent(roleCode)) {
            if (!SchoolRoleCodes.ALL.contains(roleCode)) {
                errors.add(error("roleCode", "Vai trò không hợp lệ, chỉ chấp nhận STUDENT hoặc TEACHER"));
            } else if (rolesByCode.get(roleCode) == null) {
                errors.add(error("roleCode", "Không tìm thấy vai trò"));
            }
        }

        validateDateField(errors, data, "dateOfBirth", "Ngày sinh không hợp lệ");
        validateDateField(errors, data, "startDate", "Ngày bắt đầu không hợp lệ");
        validateDateField(errors, data, "endDate", "Ngày kết thúc không hợp lệ");

        return errors;
    }

    private void validateDateField(List<Map<String, String>> errors, Map<String, String> data,
            String field, String message) {
        var value = data.get(field);
        if (isPresent(value) && parseDate(value) == null) {
            errors.add(error(field, message));
        }
    }

    private void addMissingError(List<Map<String, String>> errors, Map<String, String> data,
            String field, String message) {
        if (!isPresent(data.get(field))) {
            errors.add(error(field, message));
        }
    }

    private void addIfPresent(Set<String> values, String value) {
        if (isPresent(value)) {
            values.add(value);
        }
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private String trimOrNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return DateMapper.toLocalDate(value.strip());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private record RowContext(ImportRow row, Map<String, String> normalized) {}
}
