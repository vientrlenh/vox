package com.sep.vox.application.port.input.usecase.schooluser;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sep.vox.application.common.importer.ImportFileFormat;
import com.sep.vox.application.common.importer.ImportParserFactory;
import com.sep.vox.application.common.importer.ImportRow;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ImportFieldMapping;
import com.sep.vox.application.port.input.command.PreviewSchoolUserImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.SchoolUserImportFileStoragePort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.CreateImportSessionResponse;
import com.sep.vox.application.response.input.schooluser.SchoolUserImportError;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.infrastructure.adapter.SchoolUserImportValidator;

import tools.jackson.databind.ObjectMapper;

@Service
public class PreviewSchoolUserImportFromFileUseCase implements IUseCase<PreviewSchoolUserImportFromFileCommand, CreateImportSessionResponse> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserImportFileStoragePort fileStoragePort;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final ImportParserFactory importParserFactory;
    private final SchoolUserImportValidator importValidator;
    private final ObjectMapper objectMapper;

    public PreviewSchoolUserImportFromFileUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            SchoolUserImportFileStoragePort fileStoragePort,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            ImportParserFactory importParserFactory,
            SchoolUserImportValidator importValidator,
            ObjectMapper objectMapper) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.fileStoragePort = fileStoragePort;
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.importParserFactory = importParserFactory;
        this.importValidator = importValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public CreateImportSessionResponse execute(PreviewSchoolUserImportFromFileCommand input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();
        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        SchoolUserStatusValidator.requireActive(caller);
        if (!input.schoolId().equals(caller.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        schoolRepository.findById(input.schoolId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

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
        var rowEntities = new ArrayList<com.sep.vox.domain.model.importfile.ImportRow>();
        var seenEmails = new HashSet<String>();
        var seenPhones = new HashSet<String>();
        var mapping = input.mapping() != null ? input.mapping() : Map.<String, ImportFieldMapping>of();

        for (var row : rows) {
            var validation = importValidator.validateAndPrepareRow(row, format, mapping, input.defaultRole(), seenEmails, seenPhones, false);
            allErrors.addAll(validation.errors());
            rowEntities.add(new com.sep.vox.domain.model.importfile.ImportRow(
                session.getId(),
                row.rowNumber(),
                toJson(rawRowPayload(row, format)),
                toJson(validation.mappedPayload()),
                toJson(validation.errors()),
                validation.errors().isEmpty() ? ImportRowStatus.VALID : ImportRowStatus.INVALID
            ));
        }

        importRowRepository.saveAll(rowEntities);
        session.setValidRows((int) rowEntities.stream().filter(item -> item.getStatus() == ImportRowStatus.VALID).count());
        session.setInvalidRows(allErrors.stream().map(SchoolUserImportError::rowNumber).distinct().count());
        session.setUpdatedAt(now);
        session.setUpdatedBy(callerId);
        importSessionRepository.save(session);

        return new CreateImportSessionResponse(session.getId());
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
            throw new IllegalStateException("Không thể xử lý dữ liệu preview", e);
        }
    }

}
