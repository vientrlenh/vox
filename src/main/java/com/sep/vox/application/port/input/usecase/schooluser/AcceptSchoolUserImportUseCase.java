package com.sep.vox.application.port.input.usecase.schooluser;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.SchoolUserPasswordSetUpEmailRequestedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AcceptSchoolUserImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptSchoolUserImportResponse;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

@Service
public class AcceptSchoolUserImportUseCase implements IUseCase<AcceptSchoolUserImportCommand, AcceptSchoolUserImportResponse> {

    private static final Set<String> REQUIRED_FIELDS = Set.of("email", "fullName", "roleCode", "phone", "dateOfBirth", "startDate", "endDate", "address");
    private static final Set<String> SUPPORTED_FIELDS = Set.of("email", "fullName", "roleCode", "phone", "dateOfBirth", "startDate", "endDate", "address", "studentId");

    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordSetUpTokenPort passwordSetUpTokenPort;
    private final PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private final EventPublisherPort eventPublisherPort;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final TransactionTemplate transactionTemplate;

    public AcceptSchoolUserImportUseCase(
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolRepository schoolRepository,
            PasswordSetUpTokenPort passwordSetUpTokenPort,
            PasswordSetUpTokenRepository passwordSetUpTokenRepository,
            EventPublisherPort eventPublisherPort,
            UserContextPort userContextPort,
            JsonSerializationPort jsonSerializationPort,
            PlatformTransactionManager transactionManager) {
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolRepository = schoolRepository;
        this.passwordSetUpTokenPort = passwordSetUpTokenPort;
        this.passwordSetUpTokenRepository = passwordSetUpTokenRepository;
        this.eventPublisherPort = eventPublisherPort;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public AcceptSchoolUserImportResponse execute(AcceptSchoolUserImportCommand input) {
        validateCommand(input);

        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateRequestedSchool(input.schoolId(), schoolId);
        var school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học không hoạt động");
        }

        var session = findSession(input.importSessionId());
        validateSession(session, schoolId, now);
        validateRequiredMapping(input.confirmedMapping());

        session.setStatus(ImportSessionStatus.IMPORTING);
        session.setConfirmedMappingJson(jsonSerializationPort.toJson(input.confirmedMapping()));
        session.setUpdatedAt(now);
        session.setUpdatedBy(currentUserId);
        importSessionRepository.save(session);

        var rows = importRowRepository.findBySessionIdOrderByRowNumber(session.getId());
        var seenEmails = new HashSet<String>();
        var seenStudentIds = new HashSet<String>();
        var importedCount = 0L;
        var invalidCount = 0L;

        for (var row : rows) {
            var rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            var mappedData = mapRawData(rawData, input.confirmedMapping());
            var normalized = normalize(mappedData);
            var errors = validateRow(normalized, schoolId, seenEmails, seenStudentIds);

            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            if (!errors.isEmpty()) {
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                row.setStatus(ImportRowStatus.INVALID);
                invalidCount++;
                continue;
            }

            var roleCode = normalized.get("roleCode");
            var role = roleRepository.findByCode(roleCode).orElse(null);
            if (role == null) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("roleCode", "Không tìm thấy vai trò"))));
                row.setStatus(ImportRowStatus.INVALID);
                invalidCount++;
                continue;
            }

            final var normalizedData = normalized;
            final var schoolName = school.getName();
            UUID createdId;
            try {
                createdId = transactionTemplate.execute(status -> {
                    var ts = OffsetDateTime.now();
                    var dateOfBirth = parseDate(normalizedData.get("dateOfBirth"));
                    User user = "STUDENT".equals(roleCode)
                        ? User.createStudent(normalizedData.get("email"), normalizedData.get("phone"), normalizedData.get("fullName"), dateOfBirth, normalizedData.get("address"), null, currentUserId, schoolId, ts)
                        : User.createTeacher(normalizedData.get("email"), normalizedData.get("phone"), normalizedData.get("fullName"), dateOfBirth, normalizedData.get("address"), null, currentUserId, schoolId, ts);

                    var savedUser = userRepository.save(user);
                    userRoleRepository.save(new UserRole(savedUser.getId(), role.getId(), ts));

                    if ("STUDENT".equals(roleCode)) {
                        var startDate = parseDate(normalizedData.get("startDate"));
                        var endDate = parseDate(normalizedData.get("endDate"));
                        var startOffset = startDate != null
                            ? startDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
                            : ts;
                        var endOffset = endDate != null
                            ? endDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
                            : OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
                        var studentId = normalizedData.get("studentId");
                        schoolUserRepository.save(SchoolUser.create(studentId, schoolId, savedUser.getId(), startOffset, endOffset));
                    }

                    var generatedToken = passwordSetUpTokenPort.generateToken();
                    passwordSetUpTokenRepository.save(PasswordSetUpToken.create(savedUser.getId(), generatedToken.hashedToken()));
                    eventPublisherPort.publish(new SchoolUserPasswordSetUpEmailRequestedEvent(
                        normalizedData.get("email"),
                        normalizedData.get("fullName"),
                        schoolName,
                        savedUser.getId(),
                        generatedToken.rawToken()
                    ));
                    return savedUser.getId();
                });
            } catch (DataIntegrityViolationException e) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("email", "Email hoặc số điện thoại đã tồn tại"))));
                row.setStatus(ImportRowStatus.INVALID);
                invalidCount++;
                continue;
            }

            if (createdId != null) {
                row.setErrorsJson(null);
                row.setStatus(ImportRowStatus.IMPORTED);
                importedCount++;
            }
        }

        importRowRepository.saveAll(rows);

        session.setImportedRows(importedCount);
        session.setInvalidRows(invalidCount);
        session.setValidRows(importedCount);
        session.setSkippedRows(0L);
        session.setTotalRows(rows.size());
        session.setStatus(ImportSessionStatus.COMPLETED);
        session.setUpdatedAt(now);
        session.setUpdatedBy(currentUserId);
        var savedSession = importSessionRepository.save(session);

        return new AcceptSchoolUserImportResponse(
            savedSession.getId(),
            savedSession.getTotalRows(),
            savedSession.getImportedRows(),
            savedSession.getInvalidRows(),
            savedSession.getSkippedRows(),
            savedSession.getStatus().name()
        );
    }

    private void validateCommand(AcceptSchoolUserImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Phiên import không được để trống");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("Mapping import không được để trống");
        }
    }

    private User findCurrentUser(UUID currentUserId) {
        var user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Người dùng hiện tại không hoạt động");
        }
        return user;
    }

    private UUID getSchoolId(User currentUser) {
        var schoolId = currentUser.getSchoolId();
        if (schoolId == null) {
            throw new IllegalStateException("Người dùng hiện tại không thuộc trường nào");
        }
        return schoolId;
    }

    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (requestedSchoolId == null) {
            throw new IllegalArgumentException("Trường học không được để trống");
        }
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("Trường học không khớp với người dùng hiện tại");
        }
    }

    private ImportSession findSession(UUID importSessionId) {
        return importSessionRepository.findById(importSessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên import"));
    }

    private void validateSession(ImportSession session, UUID schoolId, OffsetDateTime now) {
        if (session.getType() != ImportType.USER) {
            throw new IllegalArgumentException("Phiên import không phải là import người dùng");
        }
        if (!Objects.equals(session.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Phiên import không thuộc trường hiện tại");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalStateException("Phiên import không ở trạng thái cho accept");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            session.setStatus(ImportSessionStatus.EXPIRED);
            importSessionRepository.save(session);
            throw new IllegalStateException("Phiên import đã hết hạn");
        }
    }

    private void validateRequiredMapping(Map<String, String> confirmedMapping) {
        var mappedFields = new HashSet<String>();
        confirmedMapping.values().stream()
            .filter(Objects::nonNull)
            .map(String::strip)
            .forEach(mappedFields::add);
        var missingFields = REQUIRED_FIELDS.stream()
            .filter(field -> !mappedFields.contains(field))
            .toList();
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Mapping import thiếu trường bắt buộc: " + String.join(", ", missingFields));
        }
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
        normalized.put("studentId", trimOrNull(mappedData.get("studentId")));
        return normalized;
    }

    private List<Map<String, String>> validateRow(Map<String, String> data, UUID schoolId, Set<String> seenEmails, Set<String> seenStudentIds) {
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
            } else if (userRepository.existsByEmail(email)) {
                errors.add(error("email", "Email đã tồn tại trong hệ thống"));
            }
        }

        validateDateField(errors, data, "dateOfBirth", "Ngày sinh không hợp lệ");
        validateDateField(errors, data, "startDate", "Ngày bắt đầu không hợp lệ");
        validateDateField(errors, data, "endDate", "Ngày kết thúc không hợp lệ");

        var studentId = data.get("studentId");
        if (isPresent(studentId) && !seenStudentIds.add(studentId)) {
            errors.add(error("studentId", "Mã học sinh bị trùng trong file import"));
        }

        return errors;
    }

    private void validateDateField(List<Map<String, String>> errors, Map<String, String> data, String field, String message) {
        var value = data.get(field);
        if (isPresent(value)) {
            try {
                LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                errors.add(error(field, message));
            }
        }
    }

    private void addMissingError(List<Map<String, String>> errors, Map<String, String> data, String field, String message) {
        if (!isPresent(data.get(field))) {
            errors.add(error(field, message));
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
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }
}
