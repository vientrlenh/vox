package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.school.School;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.application.exception.DataConflictException;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.UserCreatedPayloadV1;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.common.UserTypeConstant;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.OutboxRepository;
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
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final RequiresNewTransactionPort requiresNewTransactionPort;

    public SchoolUserImportCommitHandler(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolRepository schoolRepository,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort,
            RequiresNewTransactionPort requiresNewTransactionPort) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolRepository = schoolRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.requiresNewTransactionPort = requiresNewTransactionPort;
    }

    @Override
    public ImportType supportedType() {
        return ImportType.USER;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID schoolId = session.getSchoolId();
        Map<String, String> mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        return processRows(rows, mapping, schoolId, school.getName());
    }

    private ImportCommitResult processRows(List<ImportRow> rows, Map<String, String> confirmedMapping,
            UUID schoolId, String schoolName) {
        long createdRows = 0L;
        long updatedRows = 0L;
        long invalidRows = 0L;
        Set<String> seenEmails = new HashSet<String>();
        List<RowContext> rowContexts = new ArrayList<RowContext>();
        Set<String> emails = new HashSet<String>();
        Set<String> roleCodes = new HashSet<String>();

        for (ImportRow row : rows) {
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            Map<String, String> mappedData = mapRawData(rawData, confirmedMapping);
            Map<String, String> normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            rowContexts.add(new RowContext(row, normalized));
            addIfPresent(emails, normalized.get("email"));
            addIfPresent(roleCodes, normalized.get("roleCode"));
        }

        Map<String, User> existingUsersByEmail = findUsersByEmail(emails);
        Map<String, Role> rolesByCode = findRolesByCode(roleCodes);
        Map<UUID, SchoolUser> membershipsByUserId = findMembershipsByUserId(existingUsersByEmail.values());

        for (RowContext rowContext : rowContexts) {
            ImportRow row = rowContext.row();
            Map<String, String> normalized = rowContext.normalized();
            List<Map<String, String>> errors = validateRow(normalized, seenEmails, rolesByCode);

            if (!errors.isEmpty()) {
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                row.setStatus(ImportRowStatus.INVALID);
                invalidRows++;
                continue;
            }

            String email = normalized.get("email");
            String roleCode = normalized.get("roleCode");
            User existingUser = existingUsersByEmail.get(email);
            Role role = rolesByCode.get(roleCode);

            // 1 user = 1 trường: nếu email đã thuộc user của trường khác thì từ chối,
            // không ghi đè hồ sơ và không gắn user đó vào trường hiện tại.
            if (existingUser != null) {
                SchoolUser membership = membershipsByUserId.get(existingUser.getId());
                if (membership != null && !Objects.equals(membership.getSchoolId(), schoolId)) {
                    row.setErrorsJson(jsonSerializationPort.toJson(
                            List.of(error("email", "Email đã được sử dụng bởi người dùng ở trường khác"))));
                    row.setStatus(ImportRowStatus.INVALID);
                    invalidRows++;
                    continue;
                }
            }

            try {
                if (existingUser != null) {
                    requiresNewTransactionPort.runInNewTransaction(() ->
                            updateUser(existingUser, normalized, schoolId, membershipsByUserId, roleCode));
                    updatedRows++;
                } else {
                    requiresNewTransactionPort.runInNewTransaction(() ->
                            createUser(normalized, role, schoolId, schoolName));
                    createdRows++;
                }
            } catch (DataConflictException | IllegalArgumentException e) {
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

    private UUID createUser(Map<String, String> data, Role role, UUID schoolId, String schoolName) {
        Instant ts = Instant.now();
        User user = User.create(data.get("email"), data.get("phone"), data.get("fullName"),
                parseDate(data.get("dateOfBirth")), data.get("address"), null);
        User savedUser = userRepository.save(user);
        userRoleRepository.save(UserRole.builder()
                .userId(savedUser.getId())
                .roleId(role.getId())
                .createdAt(ts)
                .build());
        if (SchoolRoleCodes.STUDENT.equals(role.getCode().value())) {
            Instant startOffset = parseDate(data.get("startDate")).atStartOfDay(DateMapper.DEFAULT_INPUT_ZONE).toInstant();
            Instant endOffset = parseDate(data.get("endDate")).atStartOfDay(DateMapper.DEFAULT_INPUT_ZONE).toInstant();
            schoolUserRepository.save(SchoolUser.create(savedUser.getId(), schoolId, startOffset, endOffset));
        } else if (SchoolRoleCodes.TEACHER.equals(role.getCode().value())) {
            schoolUserRepository.save(SchoolUser.create(savedUser.getId(), schoolId, ts, null));
        }
        // The consumer generates the password set-up token right before it sends the mail, so the token is never
        // stored as plaintext in the outboxes table or in the Kafka topic.
        UserCreatedPayloadV1 event = new UserCreatedPayloadV1(
            savedUser.getId(),
            data.get("email"),
            data.get("fullName"),
            schoolName,
            UserTypeConstant.SCHOOL_USER
        );
        String payload = jsonSerializationPort.toJson(event);
        Outbox outbox = Outbox.create(
            AggregateTypeConstant.USER, 
            savedUser.getId(), 
            EventTypeConstant.USER_CREATED, 
            payload, 
            ts
        );
        outboxRepository.save(outbox);
        
        return savedUser.getId();
    }

    private UUID updateUser(User existing, Map<String, String> data, UUID schoolId,
            Map<UUID, SchoolUser> membershipsByUserId, String roleCode) {
        Instant ts = Instant.now();
        existing.setFullName(new FullName(data.get("fullName")));
        existing.setPhone(new Phone(data.get("phone")));
        existing.setDateOfBirth(new DateOfBirth(parseDate(data.get("dateOfBirth"))));
        existing.setAddress(data.get("address"));
        userRepository.save(existing);
        if (SchoolRoleCodes.STUDENT.equals(roleCode)) {
            Instant startOffset = parseDate(data.get("startDate")).atStartOfDay(DateMapper.DEFAULT_INPUT_ZONE).toInstant();
            Instant endOffset = parseDate(data.get("endDate")).atStartOfDay(DateMapper.DEFAULT_INPUT_ZONE).toInstant();
            SchoolUser schoolUser = membershipsByUserId.get(existing.getId());
            if (schoolUser != null) {
                schoolUser.setStartDate(startOffset);
                schoolUser.setEndDate(endOffset);
                schoolUserRepository.save(schoolUser);
            } else {
                schoolUserRepository.save(SchoolUser.create(existing.getId(), schoolId, startOffset, endOffset));
            }
        } else if (SchoolRoleCodes.TEACHER.equals(roleCode) && membershipsByUserId.get(existing.getId()) == null) {
            schoolUserRepository.save(SchoolUser.create(existing.getId(), schoolId, ts, null));
        }
        return existing.getId();
    }

    private Map<String, User> findUsersByEmail(Set<String> emails) {
        Map<String, User> map = new LinkedHashMap<String, User>();
        userRepository.findByEmailIn(emails)
                .forEach(user -> map.putIfAbsent(user.getEmail().value(), user));
        return map;
    }

    private Map<String, Role> findRolesByCode(Set<String> codes) {
        Map<String, Role> map = new LinkedHashMap<String, Role>();
        roleRepository.findByCodeIn(codes)
                .forEach(role -> map.putIfAbsent(role.getCode().value(), role));
        return map;
    }

    private Map<UUID, SchoolUser> findMembershipsByUserId(Collection<User> users) {
        Set<UUID> userIds = new HashSet<UUID>();
        users.forEach(user -> userIds.add(user.getId()));
        Map<UUID, SchoolUser> map = new LinkedHashMap<UUID, SchoolUser>();
        schoolUserRepository.findByUserIdIn(userIds).forEach(su -> map.putIfAbsent(su.getUserId(), su));
        return map;
    }

    private Map<String, String> mapRawData(Map<String, String> rawData, Map<String, String> confirmedMapping) {
        Map<String, String> mappedData = new LinkedHashMap<String, String>();
        rawData.forEach((originalHeader, value) -> {
            String systemField = confirmedMapping.get(originalHeader);
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
        Map<String, String> normalized = new LinkedHashMap<String, String>();
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
        List<Map<String, String>> errors = new ArrayList<Map<String, String>>();

        addMissingError(errors, data, "email", "Email không được để trống");
        addMissingError(errors, data, "fullName", "Họ tên không được để trống");
        addMissingError(errors, data, "roleCode", "Vai trò không được để trống");
        addMissingError(errors, data, "phone", "Số điện thoại không được để trống");
        addMissingError(errors, data, "dateOfBirth", "Ngày sinh không được để trống");
        addMissingError(errors, data, "address", "Địa chỉ không được để trống");

        String roleCodeForDates = data.get("roleCode");
        if (SchoolRoleCodes.STUDENT.equals(roleCodeForDates)) {
            addMissingError(errors, data, "startDate", "Ngày bắt đầu không được để trống");
            addMissingError(errors, data, "endDate", "Ngày kết thúc không được để trống");
        }

        String email = data.get("email");
        if (isPresent(email)) {
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                errors.add(error("email", "Email không hợp lệ"));
            } else if (!seenEmails.add(email)) {
                errors.add(error("email", "Email bị trùng trong file import"));
            }
        }

        String roleCode = data.get("roleCode");
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

        if (SchoolRoleCodes.STUDENT.equals(roleCodeForDates)) {
            LocalDate start = parseDate(data.get("startDate"));
            LocalDate end = parseDate(data.get("endDate"));
            if (start != null && end != null && !start.isBefore(end)) {
                errors.add(error("startDate", "Ngày bắt đầu phải trước ngày kết thúc"));
            }
        }

        return errors;
    }

    private void validateDateField(List<Map<String, String>> errors, Map<String, String> data,
            String field, String message) {
        String value = data.get(field);
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
