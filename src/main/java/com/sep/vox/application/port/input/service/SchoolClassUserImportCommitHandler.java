package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class SchoolClassUserImportCommitHandler implements ImportCommitHandler {

    private static final Set<String> SUPPORTED_FIELDS = Set.of("email", "classCode");

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final RequiresNewTransactionPort requiresNewTransactionPort;

    @Override
    public ImportType supportedType() {
        return ImportType.SCHOOL_CLASS_USER;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID schoolId = session.getSchoolId();
        UUID currentUserId = session.createdByUserId()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));
        Map<String, String> mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        validateMappingKeys(rows, mapping);
        return commitRowsAndCountOutcomes(rows, mapping, schoolId, currentUserId);
    }

    private void validateMappingKeys(List<ImportRow> rows, Map<String, String> confirmedMapping) {
        if (rows.isEmpty()) {
            return;
        }
        Set<String> validHeaders = jsonSerializationPort.toStringMap(rows.get(0).getRawDataJson()).keySet();
        List<String> invalidKeys = confirmedMapping.keySet().stream()
            .filter(key -> !validHeaders.contains(key))
            .toList();
        if (!invalidKeys.isEmpty()) {
            throw new IllegalArgumentException("Mapping chứa cột không tồn tại trong file: " + String.join(", ", invalidKeys));
        }
        List<String> invalidValues = confirmedMapping.values().stream()
            .map(value -> value == null ? null : value.strip())
            .filter(value -> value != null && !value.isEmpty() && !SUPPORTED_FIELDS.contains(value))
            .toList();
        if (!invalidValues.isEmpty()) {
            throw new IllegalArgumentException("Mapping chứa trường hệ thống không hợp lệ: " + String.join(", ", invalidValues));
        }
    }

    private ImportCommitResult commitRowsAndCountOutcomes(List<ImportRow> rows, Map<String, String> confirmedMapping,
            UUID schoolId, UUID currentUserId) {
        Set<String> emails = new HashSet<String>();
        Set<String> classCodes = new HashSet<String>();
        List<RowContext> rowContexts = prepareRowContexts(rows, confirmedMapping, emails, classCodes);

        Map<String, User> usersByEmail = findUsersByEmail(emails);
        Map<String, SchoolClass> classesByCode = findClassesByCode(schoolId, classCodes);
        RowLookups lookups = new RowLookups(schoolId, currentUserId, usersByEmail, classesByCode,
                findSchoolUsersByUserId(usersByEmail.values()),
                findExistingMembershipsByPair(usersByEmail.values(), classesByCode.values()),
                new HashSet<MembershipKey>());
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
            Set<String> emails, Set<String> classCodes) {
        List<RowContext> rowContexts = new ArrayList<RowContext>();
        for (ImportRow row : rows) {
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            Map<String, String> mappedData = mapRawData(rawData, confirmedMapping);
            Map<String, String> normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            rowContexts.add(new RowContext(row, normalized));

            addIfPresent(emails, normalized.get("email"));
            addIfPresent(classCodes, normalized.get("classCode"));
        }
        return rowContexts;
    }

    private RowOutcome commitRow(RowContext rowContext, RowLookups lookups) {
        ImportRow row = rowContext.row();
        RowValidation validation = validateRow(rowContext.normalized(), lookups.schoolId(), lookups.seenMemberships(),
                lookups.usersByEmail(), lookups.classesByCode(), lookups.schoolUsersByUserId());

        if (!validation.errors().isEmpty()) {
            row.setErrorsJson(jsonSerializationPort.toJson(validation.errors()));
            row.setStatus(ImportRowStatus.INVALID);
            return RowOutcome.INVALID;
        }

        try {
            RowOutcome outcome = saveInNewTransaction(validation.user(), validation.schoolClass(), lookups);
            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
            return outcome;
        } catch (DataConflictException | IllegalArgumentException exception) {
            row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("email", "Thành viên lớp học đã tồn tại hoặc không hợp lệ"))));
            row.setStatus(ImportRowStatus.FAILED);
            return RowOutcome.INVALID;
        }
    }

    private RowOutcome saveInNewTransaction(User user, SchoolClass schoolClass, RowLookups lookups) {
        Instant now = Instant.now();
        UUID currentUserId = lookups.currentUserId();
        SchoolClassUser existing = lookups.existingMembershipsByPair().get(membershipKey(user.getId(), schoolClass.getId()));
        if (existing != null) {
            requiresNewTransactionPort.runInNewTransaction(() -> {
                existing.activate();
                existing.setAssignedBy(currentUserId);
                schoolClassUserRepository.save(existing);
            });
            return RowOutcome.UPDATED;
        }
        requiresNewTransactionPort.runInNewTransaction(() ->
            schoolClassUserRepository.save(
                SchoolClassUser.joined(user.getId(), schoolClass.getId(), now, currentUserId)));
        return RowOutcome.CREATED;
    }

    private Map<String, User> findUsersByEmail(Set<String> emails) {
        Map<String, User> usersByEmail = new LinkedHashMap<String, User>();
        userRepository.findByEmailIn(emails)
            .forEach(user -> usersByEmail.putIfAbsent(user.getEmail().value(), user));
        return usersByEmail;
    }

    private Map<String, SchoolClass> findClassesByCode(UUID schoolId, Set<String> classCodes) {
        Map<String, SchoolClass> classesByCode = new LinkedHashMap<String, SchoolClass>();
        schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, classCodes)
            .forEach(schoolClass -> classesByCode.putIfAbsent(schoolClass.getCode().value(), schoolClass));
        return classesByCode;
    }

    private Map<UUID, SchoolUser> findSchoolUsersByUserId(Iterable<User> users) {
        Set<UUID> userIds = new HashSet<UUID>();
        users.forEach(user -> userIds.add(user.getId()));
        Map<UUID, SchoolUser> schoolUsersByUserId = new LinkedHashMap<UUID, SchoolUser>();
        schoolUserRepository.findByUserIdIn(userIds)
            .forEach(schoolUser -> schoolUsersByUserId.putIfAbsent(schoolUser.getUserId(), schoolUser));
        return schoolUsersByUserId;
    }

    private Map<String, SchoolClassUser> findExistingMembershipsByPair(Iterable<User> users, Iterable<SchoolClass> schoolClasses) {
        Set<UUID> userIds = new HashSet<UUID>();
        Set<UUID> schoolClassIds = new HashSet<UUID>();
        users.forEach(user -> userIds.add(user.getId()));
        schoolClasses.forEach(schoolClass -> schoolClassIds.add(schoolClass.getId()));
        Map<String, SchoolClassUser> existingMembershipsByPair = new LinkedHashMap<String, SchoolClassUser>();
        schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(userIds, schoolClassIds)
            .forEach(membership -> existingMembershipsByPair.putIfAbsent(membershipKey(membership.getUserId(), membership.getSchoolClassId()), membership));
        return existingMembershipsByPair;
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
        List<Map<String, String>> errors = new ArrayList<Map<String, String>>();
        addMissingError(errors, mappedData, "email", "Email không được để trống");
        addMissingError(errors, mappedData, "classCode", "Mã lớp không được để trống");

        String email = mappedData.get("email");
        String classCode = mappedData.get("classCode");
        if (isPresent(email) && isPresent(classCode) && !seenMemberships.add(new MembershipKey(email, classCode))) {
            errors.add(error("email", "Người dùng và lớp học bị trùng trong file import"));
        }

        User user = resolveUser(email, schoolId, usersByEmail, schoolUsersByUserId, errors);
        SchoolClass schoolClass = resolveSchoolClass(classCode, classesByCode, errors);
        return new RowValidation(user, schoolClass, errors);
    }

    private User resolveUser(String email, UUID schoolId, Map<String, User> usersByEmail,
            Map<UUID, SchoolUser> schoolUsersByUserId, List<Map<String, String>> errors) {
        if (!isPresent(email)) {
            return null;
        }
        User user = usersByEmail.get(email);
        if (user == null) {
            errors.add(error("email", "Không tìm thấy người dùng"));
            return null;
        }
        if (!user.canBeAssignedToSchoolClass()) {
            errors.add(error("email", "Người dùng đã bị khoá hoặc vô hiệu hoá"));
        }
        SchoolUser schoolUser = schoolUsersByUserId.get(user.getId());
        if (schoolUser == null || !Objects.equals(schoolUser.getSchoolId(), schoolId)) {
            errors.add(error("email", "Người dùng không thuộc trường hiện tại"));
        }
        return user;
    }

    private SchoolClass resolveSchoolClass(String classCode, Map<String, SchoolClass> classesByCode,
            List<Map<String, String>> errors) {
        if (!isPresent(classCode)) {
            return null;
        }
        SchoolClass schoolClass = classesByCode.get(classCode);
        if (schoolClass == null) {
            errors.add(error("classCode", "Không tìm thấy lớp học"));
        } else if (schoolClass.getStatus() != SchoolClassStatus.ACTIVE) {
            errors.add(error("classCode", "Lớp học không hoạt động"));
        }
        return schoolClass;
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

    private enum RowOutcome { CREATED, UPDATED, INVALID }

    private record RowLookups(
            UUID schoolId,
            UUID currentUserId,
            Map<String, User> usersByEmail,
            Map<String, SchoolClass> classesByCode,
            Map<UUID, SchoolUser> schoolUsersByUserId,
            Map<String, SchoolClassUser> existingMembershipsByPair,
            Set<MembershipKey> seenMemberships) {
    }

    private record RowContext(ImportRow row, Map<String, String> normalized) {
    }

    private record RowValidation(User user, SchoolClass schoolClass, List<Map<String, String>> errors) {
    }

    private record MembershipKey(String email, String classCode) {
    }
}
