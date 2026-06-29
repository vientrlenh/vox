package com.sep.vox.application.port.input.usecase.schoolclassuser;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.PreviewSchoolClassUserImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassUserImportResponse;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class PreviewSchoolClassUserImportFromFileUseCase implements IUseCase<PreviewSchoolClassUserImportFromFileCommand, PreviewSchoolClassUserImportResponse> {

    private static final int SESSION_EXPIRY_DAYS = 1;

    private final FileProcessingPort fileProcessingPort;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final SchoolUserRepository schoolUserRepository;

    public PreviewSchoolClassUserImportFromFileUseCase(
            FileProcessingPort fileProcessingPort,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            JsonSerializationPort jsonSerializationPort,
            SchoolUserRepository schoolUserRepository) {
        this.fileProcessingPort = fileProcessingPort;
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public PreviewSchoolClassUserImportResponse execute(PreviewSchoolClassUserImportFromFileCommand input) {
        if (input == null || input.file() == null) {
            throw new IllegalArgumentException("File import không được để trống");
        }

        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateRequestedSchool(input.schoolId(), schoolId);
        validateSchool(schoolId);

        var parsed = fileProcessingPort.parse(input.file(), ImportType.SCHOOL_CLASS_USER);
        var expiresAt = now.plusDays(SESSION_EXPIRY_DAYS);
        var savedSession = importSessionRepository.save(createSession(input, parsed, schoolId, currentUserId, now, expiresAt));
        saveRows(savedSession.getId(), parsed);

        return new PreviewSchoolClassUserImportResponse(
            savedSession.getId(),
            safeFileName(input.file().fileName()),
            parsed.originalHeaders(),
            parsed.suggestedMapping(),
            parsed.sampleRows(),
            parsed.totalRows(),
            expiresAt.toString()
        );
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
        return schoolUserRepository.findByUserId(currentUser.getId())
            .map(su -> su.getSchoolId())
            .orElseThrow(() -> new IllegalStateException("Người dùng hiện tại không thuộc trường nào"));
    }

    private void validateSchool(UUID schoolId) {
        var school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học không hoạt động");
        }
    }

    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (requestedSchoolId == null) {
            throw new IllegalArgumentException("Trường học không được để trống");
        }
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("Trường học không khớp với người dùng hiện tại");
        }
    }

    private ImportSession createSession(
            PreviewSchoolClassUserImportFromFileCommand input,
            ParseImportFileResult parsed,
            UUID schoolId,
            UUID currentUserId,
            OffsetDateTime now,
            OffsetDateTime expiresAt) {
        return new ImportSession(
            schoolId,
            ImportType.SCHOOL_CLASS_USER,
            safeFileName(input.file().fileName()),
            jsonSerializationPort.toJson(parsed.originalHeaders()),
            jsonSerializationPort.toJson(parsed.suggestedMapping()),
            null,
            0L,
            0L,
            0L,
            0L,
            parsed.totalRows(),
            null,
            ImportSessionStatus.PREVIEWED,
            null,
            expiresAt, 
            null, 
            null, 
            null, 
            0, 
            now,
            now,
            currentUserId,
            currentUserId
        );
    }

    private void saveRows(UUID sessionId, ParseImportFileResult parsed) {
        if (parsed.rows().isEmpty()) {
            return;
        }
        var rows = new java.util.ArrayList<ImportRow>();
        var rowNumber = 1L;
        for (Map<String, String> rawRow : parsed.rows()) {
            rows.add(new ImportRow(
                sessionId,
                rowNumber,
                jsonSerializationPort.toJson(rawRow),
                null,
                null,
                ImportRowStatus.PENDING
            ));
            rowNumber++;
        }
        importRowRepository.saveAll(rows);
    }

    private String safeFileName(String fileName) {
        return fileName == null || fileName.isBlank() ? "import-file" : fileName;
    }
}
