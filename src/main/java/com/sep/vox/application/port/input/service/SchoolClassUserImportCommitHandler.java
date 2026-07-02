package com.sep.vox.application.port.input.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class SchoolClassUserImportCommitHandler implements ImportCommitHandler {

    private static final Set<String> SUPPORTED_FIELDS = Set.of("email", "classCode");

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final TransactionTemplate transactionTemplate;

    public SchoolClassUserImportCommitHandler(
            UserRepository userRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            SchoolUserRepository schoolUserRepository,
            JsonSerializationPort jsonSerializationPort,
            PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public ImportType supportedType() {
        return ImportType.SCHOOL_CLASS_USER;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        var schoolId = session.getSchoolId();
        var currentUserId = session.getCreatedBy();
        var mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        validateMappingKeys(rows, mapping);
        return processRows(rows, mapping, schoolId, currentUserId);
    }

    private void validateMappingKeys(List<ImportRow> rows, Map<String, String> confirmedMapping) {
        if (rows.isEmpty()) {
            return;
        }
        var validHeaders = jsonSerializationPort.toStringMap(rows.get(0).getRawDataJson()).keySet();
        var invalidKeys = confirmedMapping.keySet().stream()
            .filter(key -> !validHeaders.contains(key))
            .toList();
        if (!invalidKeys.isEmpty()) {
            throw new IllegalArgumentException("Mapping chứa cột không tồn tại trong file: " + String.join(", ", invalidKeys));
        }
        var invalidValues = confirmedMapping.values().stream()
            .map(value -> value == null ? null : value.strip())
            .filter(value -> value != null && !value.isEmpty() && !SUPPORTED_FIELDS.contains(value))
            .toList();
        if (!invalidValues.isEmpty()) {
            throw new IllegalArgumentException("Mapping chứa trường hệ thống không hợp lệ: " + String.join(", ", invalidValues));
        }
    }

    private ImportCommitResult processRows(List<ImportRow> rows, Map<String, String> confirmedMapping, UUID schoolId, UUID currentUserId) {
        var createdRows = 0L;
        var updatedRows = 0L;
        var invalidRows = 0L;
        var seenMemberships = new HashSet<MembershipKey>();
        var rowContexts = new ArrayList<RowContext>();
        var emails = new HashSet<String>();
        var classCodes = new HashSet<String>();

        for (var row : rows) {
            var rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            var mappedData = mapRawData(rawData, confirmedMapping);
            var normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            rowContexts.add(new RowContext(row, normalized));

            addIfPresent(emails, normalized.get("email"));
            addIfPresent(classCodes, normalized.get("classCode"));
        }

        var usersByEmail = findUsersByEmail(emails);
        var classesByCode = findClassesByCode(schoolId, classCodes);
        var schoolUsersByUserId = findSchoolUsersByUserId(usersByEmail.values());
        var existingMembershipsByPair = findExistingMembershipsByPair(usersByEmail.values(), classesByCode.values());

        for (var rowContext : rowContexts) {
            var row = rowContext.row();
            var normalized = rowContext.normalized();
            var validation = validateRow(normalized, schoolId, seenMemberships, usersByEmail, classesByCode, schoolUsersByUserId);

            if (!validation.errors().isEmpty()) {
                row.setErrorsJson(jsonSerializationPort.toJson(validation.errors()));
                row.setStatus(ImportRowStatus.INVALID);
                invalidRows++;
                continue;
            }

            var now = OffsetDateTime.now();
            var user = validation.user();
            var schoolClass = validation.schoolClass();
            var key = membershipKey(user.getId(), schoolClass.getId());
            var existing = existingMembershipsByPair.get(key);

            try {
                if (existing != null) {
                    transactionTemplate.executeWithoutResult(status -> {
                        existing.activate();
                        existing.setAssignedBy(currentUserId);
                        schoolClassUserRepository.save(existing);
                    });
                    updatedRows++;
                } else {
                    transactionTemplate.executeWithoutResult(status ->
                        schoolClassUserRepository.save(new SchoolClassUser(
                            user.getId(), schoolClass.getId(), true, now, null, currentUserId)));
                    createdRows++;
                }
            } catch (DataIntegrityViolationException | IllegalArgumentException exception) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("email", "Thành viên lớp học đã tồn tại hoặc không hợp lệ"))));
                row.setStatus(ImportRowStatus.FAILED);
                invalidRows++;
                continue;
            }

            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
        }

        return new ImportCommitResult(createdRows, updatedRows, 0L, invalidRows);
    }

    private Map<String, User> findUsersByEmail(Set<String> emails) {
        var usersByEmail = new LinkedHashMap<String, User>();
        userRepository.findByEmailIn(emails)
            .forEach(user -> usersByEmail.putIfAbsent(user.getEmail().value(), user));
        return usersByEmail;
    }

    private Map<String, SchoolClass> findClassesByCode(UUID schoolId, Set<String> classCodes) {
        var classesByCode = new LinkedHashMap<String, SchoolClass>();
        schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, classCodes)
            .forEach(schoolClass -> classesByCode.putIfAbsent(schoolClass.getCode().value(), schoolClass));
        return classesByCode;
    }

    private Map<UUID, SchoolUser> findSchoolUsersByUserId(Iterable<User> users) {
        var userIds = new HashSet<UUID>();
        users.forEach(user -> userIds.add(user.getId()));
        var schoolUsersByUserId = new LinkedHashMap<UUID, SchoolUser>();
        schoolUserRepository.findByUserIdIn(userIds)
            .forEach(schoolUser -> schoolUsersByUserId.putIfAbsent(schoolUser.getUserId(), schoolUser));
        return schoolUsersByUserId;
    }

    private Map<String, SchoolClassUser> findExistingMembershipsByPair(Iterable<User> users, Iterable<SchoolClass> schoolClasses) {
        var userIds = new HashSet<UUID>();
        var schoolClassIds = new HashSet<UUID>();
        users.forEach(user -> userIds.add(user.getId()));
        schoolClasses.forEach(schoolClass -> schoolClassIds.add(schoolClass.getId()));
        var existingMembershipsByPair = new LinkedHashMap<String, SchoolClassUser>();
        schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(userIds, schoolClassIds)
            .forEach(membership -> existingMembershipsByPair.putIfAbsent(membershipKey(membership.getUserId(), membership.getSchoolClassId()), membership));
        return existingMembershipsByPair;
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
        normalized.put("email", normalizeEmail(mappedData.get("email")));
        normalized.put("classCode", StringNormalization.normalizeCode(mappedData.get("classCode")));
        return normalized;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    private RowValidation validateRow(Map<String, String> mappedData, UUID schoolId, Set<MembershipKey> seenMemberships,
            Map<String, User> usersByEmail, Map<String, SchoolClass> classesByCode,
            Map<UUID, SchoolUser> schoolUsersByUserId) {
        var errors = new ArrayList<Map<String, String>>();
        addMissingError(errors, mappedData, "email", "Email không được để trống");
        addMissingError(errors, mappedData, "classCode", "Mã lớp không được để trống");

        var email = mappedData.get("email");
        var classCode = mappedData.get("classCode");
        if (isPresent(email) && isPresent(classCode) && !seenMemberships.add(new MembershipKey(email, classCode))) {
            errors.add(error("email", "Người dùng và lớp học bị trùng trong file import"));
        }

        User user = null;
        if (isPresent(email)) {
            user = usersByEmail.get(email);
            if (user == null) {
                errors.add(error("email", "Không tìm thấy người dùng"));
            } else {
                if (user.getStatus() != UserStatus.ACTIVE) {
                    errors.add(error("email", "Người dùng không hoạt động"));
                }
                var schoolUser = schoolUsersByUserId.get(user.getId());
                if (schoolUser == null || !Objects.equals(schoolUser.getSchoolId(), schoolId)) {
                    errors.add(error("email", "Người dùng không thuộc trường hiện tại"));
                }
            }
        }

        SchoolClass schoolClass = null;
        if (isPresent(classCode)) {
            schoolClass = classesByCode.get(classCode);
            if (schoolClass == null) {
                errors.add(error("classCode", "Không tìm thấy lớp học"));
            } else if (schoolClass.getStatus() != SchoolClassStatus.ACTIVE) {
                errors.add(error("classCode", "Lớp học không hoạt động"));
            }
        }

        return new RowValidation(user, schoolClass, errors);
    }

    private void addMissingError(List<Map<String, String>> errors, Map<String, String> mappedData, String field, String message) {
        if (!isPresent(mappedData.get(field))) {
            errors.add(error(field, message));
        }
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private void addIfPresent(Set<String> values, String value) {
        if (isPresent(value)) {
            values.add(value);
        }
    }

    private String membershipKey(UUID userId, UUID schoolClassId) {
        return userId + "|" + schoolClassId;
    }

    private Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private record RowContext(ImportRow row, Map<String, String> normalized) {
    }

    private record RowValidation(User user, SchoolClass schoolClass, List<Map<String, String>> errors) {
    }

    private record MembershipKey(String email, String classCode) {
    }
}
