package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.PreviewRubricResultBandImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewRubricResultBandImportResponse;
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
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class PreviewSystemRubricResultBandImportFromFileUseCase implements IUseCase<PreviewRubricResultBandImportCommand, PreviewRubricResultBandImportResponse> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final FileProcessingPort fileProcessingPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public PreviewSystemRubricResultBandImportFromFileUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            FileProcessingPort fileProcessingPort,
            JsonSerializationPort jsonSerializationPort,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricVersionRepository = rubricVersionRepository;
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
    public PreviewRubricResultBandImportResponse execute(PreviewRubricResultBandImportCommand command) {
        // Bước 1: Xác thực tài khoản Admin hệ thống
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // Bước 2: Bảo mật dữ liệu - Truy ngược kiểm tra quyền sở hữu Rubric
        var version = rubricVersionRepository.findById(command.rubricVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Rubric yêu cầu."));
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc tương ứng."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Bảo mật: Bộ Rubric này thuộc về trường học. System Admin không được can thiệp.");
        }

        // Bước 3: Đọc và bóc tách dữ liệu file Excel
        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.RUBRIC_RESULT_BAND);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("File tải lên trống hoặc không chứa dữ liệu hợp lệ.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS);

        // Bước 4: Tạo phiên Import làm việc (ImportSession) gắn cờ loại dữ liệu mới
        ImportSession session = new ImportSession(
                null,
                null, // System Admin luôn truyền mã trường là null
                ImportType.RUBRIC_RESULT_BAND,
                command.file().fileName(),
                jsonSerializationPort.toJson(parsedResult.originalHeaders()),
                jsonSerializationPort.toJson(parsedResult.suggestedMapping()),
                null, 0, 0, 0, 0,
                parsedResult.rows().size(),
                null,
                ImportSessionStatus.PREVIEWED, // Cấu hình trạng thái chờ Map cột ở màn Preview
                command.rubricVersionId(),
                expiresAt,
                null, null, null, 0,
                now, now, currentUserId, currentUserId
        );

        ImportSession savedSession = importSessionRepository.save(session);
        UUID generatedSessionId = savedSession.getId();

        // Bước 5: Chuyển dữ liệu Excel thô của từng hàng vào bảng ImportRow để lưu trữ tạm thời
        List<ImportRow> importRows = IntStream.range(0, parsedResult.rows().size())
                .mapToObj(i -> new ImportRow(
                        null,
                        generatedSessionId,
                        i + 1,
                        jsonSerializationPort.toJson(parsedResult.rows().get(i)),
                        null,
                        null,
                        ImportRowStatus.PENDING
                )).toList();

        importRowRepository.saveAll(importRows);

        // Bước 6: Trả thông tin cấu trúc dữ liệu thô về cho UI
        return new PreviewRubricResultBandImportResponse(
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