package com.sep.vox.application.port.input.usecase.schooluser;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.common.importer.ImportFileFormat;
import com.sep.vox.application.common.importer.ImportParserFactory;
import com.sep.vox.application.common.importer.ImportRow;
import com.sep.vox.application.event.SchoolUserPasswordSetUpEmailRequestedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ImportFieldMapping;
import com.sep.vox.application.port.input.command.ImportSchoolUsersCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.application.port.output.SchoolUserImportFileStoragePort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schooluser.SchoolUserImportError;
import com.sep.vox.application.response.input.schooluser.SchoolUserImportResponse;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.infrastructure.adapter.SchoolUserImportValidator;

import tools.jackson.databind.ObjectMapper;

@Service
public class ImportSchoolUsersUseCase implements IUseCase<ImportSchoolUsersCommand, SchoolUserImportResponse> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolUserImportFileStoragePort fileStoragePort;
    private final SchoolRepository schoolRepository;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final PasswordSetUpTokenPort passwordSetUpTokenPort;
    private final PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private final EventPublisherPort eventPublisherPort;
    private final TransactionTemplate transactionTemplate;
    private final ImportParserFactory importParserFactory;
    private final SchoolUserImportValidator importValidator;
    private final ObjectMapper objectMapper;

    public ImportSchoolUsersUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolUserImportFileStoragePort fileStoragePort,
            SchoolRepository schoolRepository,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            PasswordSetUpTokenPort passwordSetUpTokenPort,
            PasswordSetUpTokenRepository passwordSetUpTokenRepository,
            EventPublisherPort eventPublisherPort,
            PlatformTransactionManager transactionManager,
            ImportParserFactory importParserFactory,
            SchoolUserImportValidator importValidator,
            ObjectMapper objectMapper) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.fileStoragePort = fileStoragePort;
        this.schoolRepository = schoolRepository;
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.passwordSetUpTokenPort = passwordSetUpTokenPort;
        this.passwordSetUpTokenRepository = passwordSetUpTokenRepository;
        this.eventPublisherPort = eventPublisherPort;
        this.importParserFactory = importParserFactory;
        this.importValidator = importValidator;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public SchoolUserImportResponse execute(ImportSchoolUsersCommand input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();
        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        SchoolUserStatusValidator.requireActive(caller);
        if (!input.schoolId().equals(caller.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var school = schoolRepository.findById(input.schoolId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

        var preview = previewImportRows(input, callerId, caller);
        try {
            var errors = new ArrayList<>(preview.errors());
            var createdUserIds = new ArrayList<UUID>();
            int failedCount = preview.failedCount();
            int createdCount = 0;

            if (!input.dryRun()) {
                var session = preview.session();
                session.setStatus(ImportSessionStatus.IMPORTING);
                session.setUpdatedAt(OffsetDateTime.now());
                session.setUpdatedBy(callerId);
                importSessionRepository.save(session);

                for (var validRow : preview.validRows()) {
                    var role = roleRepository.findByCode(validRow.roleCode()).orElse(null);
                    if (role == null) {
                        failedCount++;
                        errors.add(error((int) validRow.rowNumber(), "roleCode", "NOT_FOUND", "Không tìm thấy vai trò", validRow.roleCode()));
                        importRowRepository.save(markRowAsFailed(validRow.rowEntity(), List.of(
                            error((int) validRow.rowNumber(), "roleCode", "NOT_FOUND", "Không tìm thấy vai trò", validRow.roleCode())
                        )));
                        continue;
                    }

                    UUID createdId;
                    try {
                        createdId = transactionTemplate.execute(status -> {
                        var now = OffsetDateTime.now();
                            User user = validRow.roleCode().equals("STUDENT")
                                ? User.createStudent(validRow.email(), validRow.phone(), validRow.fullName(), validRow.dateOfBirth(), validRow.address(), null, callerId, input.schoolId(), now)
                                : User.createTeacher(validRow.email(), validRow.phone(), validRow.fullName(), validRow.dateOfBirth(), validRow.address(), null, callerId, input.schoolId(), now);

                            var savedUser = userRepository.save(user);
                            userRoleRepository.save(new UserRole(savedUser.getId(), role.getId(), now));
                            if ("STUDENT".equals(validRow.roleCode()) && validRow.studentId() != null) {
                                var startDate = validRow.startDate() != null
                                    ? validRow.startDate().atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
                                    : now;
                                var endDate = validRow.endDate() != null
                                    ? validRow.endDate().atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
                                    : OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
                                schoolUserRepository.save(SchoolUser.create(validRow.studentId(), input.schoolId(), savedUser.getId(), startDate, endDate));
                            }
                            var generatedPasswordSetUpToken = passwordSetUpTokenPort.generateToken();
                            passwordSetUpTokenRepository.save(PasswordSetUpToken.create(savedUser.getId(), generatedPasswordSetUpToken.hashedToken()));
                            eventPublisherPort.publish(new SchoolUserPasswordSetUpEmailRequestedEvent(
                                validRow.email(),
                                validRow.fullName(),
                                school.getName(),
                                savedUser.getId(),
                                generatedPasswordSetUpToken.rawToken()
                            ));
                            return savedUser.getId();
                        });
                    } catch (DataIntegrityViolationException e) {
                        failedCount++;
                        var rowErrors = List.of(
                            error((int) validRow.rowNumber(), "email", "DUPLICATE", "Email hoặc số điện thoại đã tồn tại", validRow.email())
                        );
                        errors.addAll(rowErrors);
                        importRowRepository.save(markRowAsFailed(validRow.rowEntity(), rowErrors));
                        continue;
                    }

                    if (createdId != null) {
                        createdUserIds.add(createdId);
                        createdCount++;
                        importRowRepository.save(markRowAsImported(validRow.rowEntity(), Map.of("createdUserId", createdId.toString())));
                    }
                }
            }

            var totalRows = preview.totalRows();
            var processedRows = totalRows;
            var skippedCount = totalRows - createdCount - failedCount;

            var finalSession = preview.session();
            finalSession.setImportedRows(createdCount);
            finalSession.setSkippedRows(skippedCount);
            finalSession.setUpdatedAt(OffsetDateTime.now());
            finalSession.setUpdatedBy(callerId);
            if (input.dryRun()) {
                finalSession.setStatus(ImportSessionStatus.PREVIEWED);
                finalSession.setFailureReason(null);
            } else {
                finalSession.setInvalidRows(failedCount);
                finalSession.setStatus(failedCount >= totalRows ? ImportSessionStatus.FAILED : ImportSessionStatus.COMPLETED);
                finalSession.setFailureReason(failedCount >= totalRows ? "Không có dòng hợp lệ để import" : null);
            }
            importSessionRepository.save(finalSession);

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
            if (!input.dryRun()) {
                try {
                    fileStoragePort.delete(input.fileId(), input.schoolId(), callerId);
                } catch (Exception ignored) {
                    // the scheduled job should've handle this, i think
                }
            }
        }
    }

    private PreviewResult previewImportRows(ImportSchoolUsersCommand input, UUID callerId, User caller) {
        var resource = fileStoragePort.load(input.fileId(), input.schoolId(), callerId);
        var format = ImportFileFormat.valueOf(resource.format());
        var parser = importParserFactory.forFormat(format);
        List<ImportRow> rows;
        try (var inputStream = resource.inputStream()) {
            rows = parser.parse(inputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể đọc dữ liệu", e);
        }

        var now = OffsetDateTime.now();
        var session = new ImportSession(
            input.schoolId(),
            ImportType.USER,
            resource.originalFileName(),
            toJson(extractHeaders(rows, format)),
            toJson(extractSuggestedMapping(input.mapping())),
            toJson(input.mapping() != null ? input.mapping() : Map.<String, ImportFieldMapping>of()),
            0,
            0,
            0,
            0,
            rows.size(),
            null,
            ImportSessionStatus.PREVIEWED,
            null,
            resource.expiresAt(),
            now,
            now,
            callerId,
            callerId
        );
        session = importSessionRepository.save(session);

        var allErrors = new ArrayList<SchoolUserImportError>();
        var validRows = new ArrayList<ValidPreviewRow>();
        var rowEntities = new ArrayList<com.sep.vox.domain.model.importfile.ImportRow>();
        var seenEmails = new HashSet<String>();
        var seenPhones = new HashSet<String>();
        var mapping = input.mapping() != null ? input.mapping() : Map.<String, ImportFieldMapping>of();

        for (var row : rows) {
            var validation = importValidator.validateAndPrepareRow(row, format, mapping, input.defaultRole(), seenEmails, seenPhones, true);
            allErrors.addAll(validation.errors());

            var rowEntity = new com.sep.vox.domain.model.importfile.ImportRow(
                session.getId(),
                row.rowNumber(),
                toJson(rawRowPayload(row, format)),
                toJson(validation.mappedPayload()),
                toJson(validation.errors()),
                validation.errors().isEmpty() ? ImportRowStatus.VALID : ImportRowStatus.INVALID
            );
            rowEntities.add(rowEntity);

            if (!validation.errors().isEmpty()) {
                continue;
            }

            validRows.add(new ValidPreviewRow(
                row.rowNumber(),
                validation.email(),
                validation.phone(),
                validation.fullName(),
                validation.dateOfBirth(),
                validation.address(),
                validation.studentId(),
                validation.roleCode(),
                validation.startDate(),
                validation.endDate(),
                rowEntity
            ));
        }

        var savedRows = importRowRepository.saveAll(rowEntities);
        var rowsByNumber = new HashMap<Long, com.sep.vox.domain.model.importfile.ImportRow>();
        for (var saved : savedRows) {
            rowsByNumber.put(saved.getRowNumber(), saved);
        }

        var savedValidRows = validRows.stream()
            .map(valid -> new ValidPreviewRow(
                valid.rowNumber(),
                valid.email(),
                valid.phone(),
                valid.fullName(),
                valid.dateOfBirth(),
                valid.address(),
                valid.studentId(),
                valid.roleCode(),
                valid.startDate(),
                valid.endDate(),
                rowsByNumber.get(valid.rowNumber())
            ))
            .toList();

        session.setValidRows(savedValidRows.size());
        session.setInvalidRows(allErrors.stream().map(SchoolUserImportError::rowNumber).distinct().count());
        session.setUpdatedAt(OffsetDateTime.now());
        session.setUpdatedBy(caller.getId());
        session = importSessionRepository.save(session);

        var failedCount = (int) allErrors.stream().map(SchoolUserImportError::rowNumber).distinct().count();
        return new PreviewResult(session, rows.size(), failedCount, allErrors, savedValidRows);
    }


    private Map<String, Object> rawRowPayload(ImportRow row, ImportFileFormat format) {
        if (format == ImportFileFormat.JSON) {
            return row.jsonValues() != null ? new HashMap<>(row.jsonValues()) : Map.of();
        }
        return row.columns() != null ? new HashMap<>(row.columns()) : Map.of();
    }

    private List<String> extractHeaders(List<ImportRow> rows, ImportFileFormat format) {
        if (rows.isEmpty()) {
            return List.of();
        }
        if (format == ImportFileFormat.JSON) {
            var headers = new LinkedHashSet<String>();
            for (var row : rows) {
                if (row.jsonValues() != null) {
                    headers.addAll(row.jsonValues().keySet());
                }
            }
            return List.copyOf(headers);
        }
        return rows.get(0).columns() != null ? new ArrayList<>(rows.get(0).columns().keySet()) : List.of();
    }

    private Map<String, String> extractSuggestedMapping(Map<String, ImportFieldMapping> mapping) {
        if (mapping == null || mapping.isEmpty()) {
            return Map.of();
        }
        var result = new HashMap<String, String>();
        for (var entry : mapping.entrySet()) {
            if (entry.getValue() != null && entry.getValue().column() != null && !entry.getValue().column().isBlank()) {
                result.put(entry.getKey(), entry.getValue().column());
            }
        }
        return result;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value != null ? value : Map.of());
        } catch (Exception e) {
            throw new IllegalStateException("Không thể chuyển dữ liệu import sang JSON", e);
        }
    }

    private com.sep.vox.domain.model.importfile.ImportRow markRowAsImported(
            com.sep.vox.domain.model.importfile.ImportRow row,
            Map<String, Object> importedMetadata) {
        row.setStatus(ImportRowStatus.IMPORTED);
        row.setErrorsJson(toJson(List.of()));
        row.setMappedDataJson(toJson(importedMetadata));
        return row;
    }

    private com.sep.vox.domain.model.importfile.ImportRow markRowAsFailed(
            com.sep.vox.domain.model.importfile.ImportRow row,
            List<SchoolUserImportError> errors) {
        row.setStatus(ImportRowStatus.FAILED);
        row.setErrorsJson(toJson(errors));
        return row;
    }

    private static SchoolUserImportError error(int rowNumber, String field, String code, String message, String rawValue) {
        return new SchoolUserImportError(rowNumber, field, code, message, rawValue);
    }

    private record ValidPreviewRow(
        long rowNumber,
        String email,
        String phone,
        String fullName,
        LocalDate dateOfBirth,
        String address,
        String studentId,
        String roleCode,
        LocalDate startDate,
        LocalDate endDate,
        com.sep.vox.domain.model.importfile.ImportRow rowEntity
    ) {
    }

    private record PreviewResult(
        ImportSession session,
        int totalRows,
        int failedCount,
        List<SchoolUserImportError> errors,
        List<ValidPreviewRow> validRows
    ) {
    }
}