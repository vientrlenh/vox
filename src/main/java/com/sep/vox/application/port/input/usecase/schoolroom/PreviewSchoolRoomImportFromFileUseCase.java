package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.domain.model.school.School;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.schoolroom.PreviewSchoolRoomImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolroom.PreviewSchoolRoomImportResponse;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PreviewSchoolRoomImportFromFileUseCase
        implements IUseCase<PreviewSchoolRoomImportFromFileCommand, PreviewSchoolRoomImportResponse> {

    private static final int SESSION_EXPIRY_DAYS = 1;

    private final FileProcessingPort fileProcessingPort;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public PreviewSchoolRoomImportResponse execute(PreviewSchoolRoomImportFromFileCommand input) {
        if (input == null || input.file() == null) {
            throw new IllegalArgumentException("The import file must not be empty");
        }

        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        UUID schoolId = getSchoolId(currentUser);
        validateRequestedSchool(input.schoolId(), schoolId);
        validateSchool(schoolId);

        ParseImportFileResult parsed = fileProcessingPort.parse(input.file(), ImportType.SCHOOL_ROOM);
        Instant expiresAt = now.plus(SESSION_EXPIRY_DAYS, ChronoUnit.DAYS);
        ImportSession savedSession = importSessionRepository.save(createSession(input, parsed, schoolId, currentUserId, now, expiresAt));
        saveRows(savedSession.getId(), parsed);

        return toPreviewResponse(savedSession, input, parsed, expiresAt);
    }

    private PreviewSchoolRoomImportResponse toPreviewResponse(
            ImportSession savedSession, PreviewSchoolRoomImportFromFileCommand input, ParseImportFileResult parsed, Instant expiresAt) {
        return new PreviewSchoolRoomImportResponse(
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
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("The current user is not active");
        }
        return user;
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("The current user does not belong to any school"));
        return schoolUser.getSchoolId();
    }

    private void validateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        if (!school.isActive()) {
            throw new IllegalArgumentException("The school is not active");
        }
    }

    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (requestedSchoolId == null) {
            throw new IllegalArgumentException("The school must not be empty");
        }
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("The school does not match the current user");
        }
    }

    private ImportSession createSession(
            PreviewSchoolRoomImportFromFileCommand input,
            ParseImportFileResult parsed,
            UUID schoolId,
            UUID currentUserId,
            Instant now,
            Instant expiresAt) {
        return ImportSession.previewed(
            schoolId,
            ImportType.SCHOOL_ROOM,
            safeFileName(input.file().fileName()),
            jsonSerializationPort.toJson(parsed.originalHeaders()),
            jsonSerializationPort.toJson(parsed.suggestedMapping()),
            parsed.totalRows(),
            expiresAt);
    }

    private void saveRows(UUID sessionId, ParseImportFileResult parsed) {
        if (parsed.rows().isEmpty()) {
            return;
        }
        List<ImportRow> rows = new ArrayList<ImportRow>();
        long rowNumber = 1L;
        for (Map<String, String> rawRow : parsed.rows()) {
            rows.add(ImportRow.pending(sessionId, rowNumber, jsonSerializationPort.toJson(rawRow)));
            rowNumber++;
        }
        importRowRepository.saveAll(rows);
    }

    private String safeFileName(String fileName) {
        return fileName == null || fileName.isBlank() ? "import-file" : fileName;
    }
}
