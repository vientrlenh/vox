package com.sep.vox.application.port.input.usecase.schoolclass;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.JsonSerialization;
import com.sep.vox.application.port.input.command.PreviewSchoolClassImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassImportResponse;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.application.exception.NotFoundException;
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
import com.sep.vox.domain.repository.UserRepository;

@Service
public class PreviewSchoolClassImportFromFileUseCase implements IUseCase<PreviewSchoolClassImportFromFileCommand, PreviewSchoolClassImportResponse> {

    private static final int SESSION_EXPIRY_DAYS = 1;

    private final FileProcessingPort fileProcessingPort;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;

    public PreviewSchoolClassImportFromFileUseCase(
            FileProcessingPort fileProcessingPort,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolRepository schoolRepository) {
        this.fileProcessingPort = fileProcessingPort;
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
    }

    @Override
    @Transactional
    public PreviewSchoolClassImportResponse execute(PreviewSchoolClassImportFromFileCommand input) {
        if (input == null || input.file() == null) {
            throw new IllegalArgumentException("File import khong duoc de trong");
        }

        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        var parsed = fileProcessingPort.parse(input.file(), ImportType.SCHOOL_CLASS);
        var expiresAt = now.plusDays(SESSION_EXPIRY_DAYS);
        var savedSession = importSessionRepository.save(createSession(input, parsed, schoolId, currentUserId, now, expiresAt));
        saveRows(savedSession.getId(), parsed);

        return new PreviewSchoolClassImportResponse(
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
            .orElseThrow(() -> new NotFoundException("Khong tim thay nguoi dung hien tai"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Nguoi dung hien tai khong hoat dong");
        }
        return user;
    }

    private UUID getSchoolId(User currentUser) {
        var schoolId = currentUser.getSchoolId();
        if (schoolId == null) {
            throw new IllegalStateException("Nguoi dung hien tai khong thuoc truong nao");
        }
        return schoolId;
    }

    private void validateSchool(UUID schoolId) {
        var school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new NotFoundException("Khong tim thay truong hoc"));
        if (!school.isActive()) {
            throw new IllegalStateException("Truong hoc khong hoat dong");
        }
    }

    private ImportSession createSession(
            PreviewSchoolClassImportFromFileCommand input,
            ParseImportFileResult parsed,
            UUID schoolId,
            UUID currentUserId,
            OffsetDateTime now,
            OffsetDateTime expiresAt) {
        return new ImportSession(
            schoolId,
            ImportType.SCHOOL_CLASS,
            safeFileName(input.file().fileName()),
            JsonSerialization.toJson(parsed.originalHeaders()),
            JsonSerialization.toJson(parsed.suggestedMapping()),
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
                JsonSerialization.toJson(rawRow),
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
