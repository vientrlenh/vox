package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.PreviewRubricVersionImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewRubricVersionImportResponse; // 👉 IMPORT THÊM RESPONSE MỚI
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class PreviewSystemRubricVersionImportFromFileUseCase implements IUseCase<PreviewRubricVersionImportFromFileCommand, PreviewRubricVersionImportResponse> { // 👉 ĐỔI KIỂU TRẢ VỀ Ở ĐÂY

    private final RubricRepository rubricRepository;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final FileProcessingPort fileProcessingPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public PreviewSystemRubricVersionImportFromFileUseCase(
            RubricRepository rubricRepository,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            FileProcessingPort fileProcessingPort,
            JsonSerializationPort jsonSerializationPort,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricRepository = rubricRepository;
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.fileProcessingPort = fileProcessingPort;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PreviewRubricVersionImportResponse execute(PreviewRubricVersionImportFromFileCommand command) { // 👉 ĐỔI KIỂU TRẢ VỀ Ở ĐÂY
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        var rubric = rubricRepository.findById(command.rubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric hệ thống yêu cầu."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Bộ Rubric này thuộc về trường học. System Admin không thể nạp Version vào đây.");
        }

        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.RUBRIC_VERSION);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("File tải lên trống hoặc không chứa dòng dữ liệu hợp lệ.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS); // Phiên hết hạn sau 1 ngày

        // 1. Tạo Session với ID = null để DB tự sinh UUIDv7
        ImportSession session = new ImportSession(
                null,
                null,
                ImportType.RUBRIC_VERSION,
                command.file().fileName(),
                jsonSerializationPort.toJson(parsedResult.originalHeaders()),
                jsonSerializationPort.toJson(parsedResult.suggestedMapping()),
                null,
                0, 0, 0, 0,
                parsedResult.rows().size(),
                null,
                ImportSessionStatus.PREVIEWED,
                command.rubricId(),
                expiresAt,
                null, null, null, 0,
                now, now, currentUserId, currentUserId
        );

        // 2. Lưu vào DB và lấy ID xịn do DB sinh ra
        ImportSession savedSession = importSessionRepository.save(session);
        UUID generatedSessionId = savedSession.getId();

        // 3. Tạo các dòng ImportRow
        List<ImportRow> importRows = IntStream.range(0, parsedResult.rows().size())
                .mapToObj(i -> {
                    var rowMap = parsedResult.rows().get(i);
                    return new ImportRow(
                            null,
                            generatedSessionId,
                            i + 1,
                            jsonSerializationPort.toJson(rowMap),
                            null,
                            null,
                            ImportRowStatus.PENDING
                    );
                })
                .toList();

        importRowRepository.saveAll(importRows);

        //  TRẢ VỀ CỤC RESPONSE FULL OPTION CHO FRONTEND
        return new PreviewRubricVersionImportResponse(
                generatedSessionId,
                command.file().fileName(),
                parsedResult.originalHeaders(),
                parsedResult.suggestedMapping(),
                parsedResult.sampleRows(),
                parsedResult.totalRows(),
                expiresAt
        );
    }
}