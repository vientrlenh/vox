package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.school.School;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.UserCreatedPayloadV1;
import com.sep.vox.application.exception.ResourceNotFoundException;
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
@RequiredArgsConstructor
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

    @Override
    public ImportType supportedType() {
        return ImportType.USER;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID schoolId = session.getSchoolId();
        Map<String, String> mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        return commitRowsAndCountOutcomes(rows, mapping, schoolId, school.getName());
    }

    private ImportCommitResult commitRowsAndCountOutcomes(List<ImportRow> rows, Map<String, String> confirmedMapping,
            UUID schoolId, String schoolName) {
        Set<String> emails = new HashSet<String>();
        Set<String> roleCodes = new HashSet<String>();
        List<RowContext> rowContexts = prepareRowContexts(rows, confirmedMapping, emails, roleCodes);

        Map<String, User> existingUsersByEmail = findUsersByEmail(emails);
        Map<String, Role> rolesByCode = findRolesByCode(roleCodes);
        RowLookups lookups = new RowLookups(schoolId, schoolName, existingUsersByEmail, rolesByCode,
                findMembershipsByUserId(existingUsersByEmail.values()), new HashSet<String>());
        Map<RowOutcome, Long> outcomeCounts = new EnumMap<RowOutcome, Long>(RowOutcome.class);
        for (RowContext rowContext : rowContexts) {
            outcomeCounts.merge(commitRow(rowContext, lookups), 1L, (a, b) -> Long.sum(a, b));
        }
        return toResult(outcomeCounts);
    }

    private ImportCommitResult toResult(Map<RowOutcome, Long> outcomeCounts) {
        return new ImportCommitResult(
                outcomeCounts.getOrDefault(RowOutcome.CREATED, 0L),
                outcomeCounts.getOrDefault(RowOutcome.UPDATED, 0L),
                0L,
                outcomeCounts.getOrDefault(RowOutcome.INVALID, 0L));
    }

    private List<RowContext> prepareRowContexts(List<ImportRow> rows, Map<String, String> confirmedMapping,
            Set<String> emails, Set<String> roleCodes) {
        List<RowContext> rowContexts = new ArrayList<RowContext>();
        for (ImportRow row : rows) {
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            Map<String, String> mappedData = mapRawData(rawData, confirmedMapping);
            Map<String, String> normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            rowContexts.add(new RowContext(row, normalized));
            addIfPresent(emails, normalized.get("email"));
            addIfPresent(roleCodes, normalized.get("roleCode"));
        }
        return rowContexts;
    }

    private RowOutcome commitRow(RowContext rowContext, RowLookups lookups) {
        ImportRow row = rowContext.row();
        Map<String, String> normalized = rowContext.normalized();
        List<Map<String, String>> errors = validateRow(normalized, lookups.seenEmails(), lookups.rolesByCode());

        if (!errors.isEmpty()) {
            row.setErrorsJson(jsonSerializationPort.toJson(errors));
            row.setStatus(ImportRowStatus.INVALID);
            return RowOutcome.INVALID;
        }

        User existingUser = lookups.existingUsersByEmail().get(normalized.get("email"));
        if (belongsToAnotherSchool(existingUser, lookups)) {
            row.setErrorsJson(jsonSerializationPort.toJson(
                    List.of(error("email", "Email is already used by a user at another school"))));
            row.setStatus(ImportRowStatus.INVALID);
            return RowOutcome.INVALID;
        }

        return importRow(row, existingUser, normalized, lookups);
    }

    private RowOutcome importRow(ImportRow row, User existingUser, Map<String, String> normalized, RowLookups lookups) {
        try {
            RowOutcome outcome = saveInNewTransaction(existingUser, normalized, lookups);
            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
            return outcome;
        } catch (DataConflictException | IllegalArgumentException e) {
            row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("email", "Email or phone number already exists or the data is invalid"))));
            row.setStatus(ImportRowStatus.FAILED);
            return RowOutcome.INVALID;
        }
    }

    // One user = one school: if the email already belongs to a user of another school, reject the row;
    // do not overwrite the profile and do not attach that user to the current school.
    private boolean belongsToAnotherSchool(User existingUser, RowLookups lookups) {
        if (existingUser == null) {
            return false;
        }
        SchoolUser membership = lookups.membershipsByUserId().get(existingUser.getId());
        return membership != null && !Objects.equals(membership.getSchoolId(), lookups.schoolId());
    }

    private RowOutcome saveInNewTransaction(User existingUser, Map<String, String> normalized, RowLookups lookups) {
        if (existingUser != null) {
            String roleCode = normalized.get("roleCode");
            requiresNewTransactionPort.runInNewTransaction(() ->
                    updateUser(existingUser, normalized, lookups.schoolId(), lookups.membershipsByUserId(), roleCode));
            return RowOutcome.UPDATED;
        }
        Role role = lookups.rolesByCode().get(normalized.get("roleCode"));
        requiresNewTransactionPort.runInNewTransaction(() ->
                createUser(normalized, role, lookups.schoolId(), lookups.schoolName()));
        return RowOutcome.CREATED;
    }

    private UUID createUser(Map<String, String> data, Role role, UUID schoolId, String schoolName) {
        Instant ts = Instant.now();
        User user = User.create(data.get("email"), data.get("phone"), data.get("fullName"),
                parseDate(data.get("dateOfBirth")), data.get("address"), null);
        User savedUser = userRepository.save(user);
        userRoleRepository.save(UserRole.assigned(savedUser.getId(), role.getId(), ts));
        if (SchoolRoleCodes.STUDENT.equals(role.getCode().value())) {
            schoolUserRepository.save(SchoolUser.create(savedUser.getId(), schoolId,
                    startOfDayInstant(data.get("startDate")), startOfDayInstant(data.get("endDate"))));
        } else if (SchoolRoleCodes.TEACHER.equals(role.getCode().value())) {
            schoolUserRepository.save(SchoolUser.create(savedUser.getId(), schoolId, ts, null));
        }
        saveUserCreatedOutbox(savedUser, data, schoolName, ts);
        return savedUser.getId();
    }

    // The consumer generates the password set-up token right before it sends the mail, so the token is never
    // stored as plaintext in the outboxes table or in the Kafka topic.
    private void saveUserCreatedOutbox(User savedUser, Map<String, String> data, String schoolName, Instant ts) {
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
    }

    private Instant startOfDayInstant(String date) {
        return parseDate(date).atStartOfDay(DateMapper.DEFAULT_INPUT_ZONE).toInstant();
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
            Instant startOffset = startOfDayInstant(data.get("startDate"));
            Instant endOffset = startOfDayInstant(data.get("endDate"));
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
        addRequiredFieldErrors(errors, data);
        addEmailErrors(errors, data.get("email"), seenEmails);
        addRoleErrors(errors, data.get("roleCode"), rolesByCode);

        validateDateField(errors, data, "dateOfBirth", "Date of birth is invalid");
        validateDateField(errors, data, "startDate", "Start date is invalid");
        validateDateField(errors, data, "endDate", "End date is invalid");

        if (SchoolRoleCodes.STUDENT.equals(data.get("roleCode"))) {
            LocalDate start = parseDate(data.get("startDate"));
            LocalDate end = parseDate(data.get("endDate"));
            if (start != null && end != null && !start.isBefore(end)) {
                errors.add(error("startDate", "Start date must be before end date"));
            }
        }

        return errors;
    }

    private void addRequiredFieldErrors(List<Map<String, String>> errors, Map<String, String> data) {
        addMissingError(errors, data, "email", "Email must not be blank");
        addMissingError(errors, data, "fullName", "Full name must not be blank");
        addMissingError(errors, data, "roleCode", "Role must not be blank");
        addMissingError(errors, data, "phone", "Phone number must not be blank");
        addMissingError(errors, data, "dateOfBirth", "Date of birth must not be blank");
        addMissingError(errors, data, "address", "Address must not be blank");

        if (SchoolRoleCodes.STUDENT.equals(data.get("roleCode"))) {
            addMissingError(errors, data, "startDate", "Start date must not be blank");
            addMissingError(errors, data, "endDate", "End date must not be blank");
        }
    }

    private void addEmailErrors(List<Map<String, String>> errors, String email, Set<String> seenEmails) {
        if (!isPresent(email)) {
            return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errors.add(error("email", "Email is invalid"));
        } else if (!seenEmails.add(email)) {
            errors.add(error("email", "Email is duplicated in the import file"));
        }
    }

    private void addRoleErrors(List<Map<String, String>> errors, String roleCode, Map<String, Role> rolesByCode) {
        if (!isPresent(roleCode)) {
            return;
        }
        if (!SchoolRoleCodes.ALL.contains(roleCode)) {
            errors.add(error("roleCode", "Role is invalid; only STUDENT or TEACHER is accepted"));
        } else if (rolesByCode.get(roleCode) == null) {
            errors.add(error("roleCode", "Role not found"));
        }
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

    private enum RowOutcome { CREATED, UPDATED, INVALID }

    private record RowLookups(
            UUID schoolId,
            String schoolName,
            Map<String, User> existingUsersByEmail,
            Map<String, Role> rolesByCode,
            Map<UUID, SchoolUser> membershipsByUserId,
            Set<String> seenEmails) {}

    private record RowContext(ImportRow row, Map<String, String> normalized) {}
}
