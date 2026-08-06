package com.sep.vox.application.port.input.usecase.framework;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.PreviewFrameworkVersionImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewFrameworkVersionImportResponse;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class PreviewFrameworkVersionImportFromFileUseCase
        implements IUseCase<PreviewFrameworkVersionImportCommand, PreviewFrameworkVersionImportResponse> {

    private final FrameworkRepository frameworkRepository;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final FileProcessingPort fileProcessingPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public PreviewFrameworkVersionImportFromFileUseCase(
            FrameworkRepository frameworkRepository,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            FileProcessingPort fileProcessingPort,
            JsonSerializationPort jsonSerializationPort,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.frameworkRepository = frameworkRepository;
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.fileProcessingPort = fileProcessingPort;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PreviewFrameworkVersionImportResponse execute(PreviewFrameworkVersionImportCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản không hoạt động.");
        }

        frameworkRepository.findById(command.frameworkId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khung năng lực."));

        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.FRAMEWORK_VERSION);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("File không có dữ liệu.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS);

        ImportSession session = new ImportSession(
                null, null, ImportType.FRAMEWORK_VERSION, command.file().fileName(),
                jsonSerializationPort.toJson(parsedResult.originalHeaders()),
                jsonSerializationPort.toJson(parsedResult.suggestedMapping()),
                null, 0, 0, 0, 0, parsedResult.rows().size(), null,
                ImportSessionStatus.PREVIEWED, command.frameworkId(), expiresAt,
                null, null, null, 0, now, now, currentUserId, currentUserId
        );
        ImportSession savedSession = importSessionRepository.save(session);
        UUID generatedSessionId = savedSession.getId();

        List<ImportRow> importRows = IntStream.range(0, parsedResult.rows().size())
                .mapToObj(i -> new ImportRow(
                        null, generatedSessionId, i + 1,
                        jsonSerializationPort.toJson(parsedResult.rows().get(i)),
                        null, null, ImportRowStatus.PENDING
                )).toList();
        importRowRepository.saveAll(importRows);

        return new PreviewFrameworkVersionImportResponse(
                generatedSessionId, command.file().fileName(), parsedResult.originalHeaders(),
                parsedResult.suggestedMapping(), parsedResult.sampleRows(), parsedResult.totalRows(), expiresAt
        );
    }
}
