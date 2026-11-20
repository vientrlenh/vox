package com.sep.vox.application.port.input.usecase.rubricschool;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream; // Sử dụng Command dùng chung của bác

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.PreviewRubricCriterionImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewRubricCriterionImportResponse;
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
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class PreviewSchoolRubricCriterionImportFromFileUseCase implements IUseCase<PreviewRubricCriterionImportCommand, PreviewRubricCriterionImportResponse> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final FileProcessingPort fileProcessingPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public PreviewSchoolRubricCriterionImportFromFileUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            FileProcessingPort fileProcessingPort,
            JsonSerializationPort jsonSerializationPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.fileProcessingPort = fileProcessingPort;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PreviewRubricCriterionImportResponse execute(PreviewRubricCriterionImportCommand command) {
        // Validation khắt khe đầu vào tránh Null Pointer bậy bạ
        if (command.schoolId() == null) {
            throw new IllegalArgumentException("Yêu cầu không hợp lệ: Thiếu mã trường học đối với luồng School Admin.");
        }

        // Bước 1: Kiểm tra trạng thái tài khoản đang đăng nhập
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }

        // Bước 2: Kiểm tra liên kết giữa tài khoản và trường học (Gác cổng bảo mật)
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Bạn không thuộc trường học nào để thực hiện chức năng này."));

        // Chống hack: Đổi schoolId trên URL khác với id trường của chính mình
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên dữ liệu của trường khác.");
        }

        // Bước 3: Kiểm tra tính tồn tại và quyền sở hữu bộ Rubric
        var version = rubricVersionRepository.findById(command.rubricVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản (Version) yêu cầu."));
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        // Chống hack: Cố tình truyền Id của một Rubric thuộc về System hoặc của một trường khác vào URL trường mình
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Bảo mật: Bộ Rubric này không thuộc sở hữu của trường bạn.");
        }

        // Bước 4: Bóc tách cấu trúc file Excel thô
        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.RUBRIC_CRITERION);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("File tải lên rỗng hoặc không có dữ liệu hợp lệ.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS);

        // Bước 5: Tạo phiên import (ImportSession) mới lưu vào DB
        ImportSession session = new ImportSession(
                null,
                command.schoolId(), // Lưu trữ chặt chẽ id trường học để phục vụ Worker bốc việc ngầm
                ImportType.RUBRIC_CRITERION,
                command.file().fileName(),
                jsonSerializationPort.toJson(parsedResult.originalHeaders()),
                jsonSerializationPort.toJson(parsedResult.suggestedMapping()),
                null,
                0, 0, 0, 0,
                parsedResult.rows().size(),
                null,
                ImportSessionStatus.PREVIEWED, // Set trạng thái sẵn sàng để user map cột
                command.rubricVersionId(),
                expiresAt,
                null, null, null, 0,
                now, now, currentUserId, currentUserId
        );

        ImportSession savedSession = importSessionRepository.save(session);
        UUID generatedSessionId = savedSession.getId();

        // Bước 6: Tạo danh sách các dòng dữ liệu Excel thô (ImportRows)
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

        // Bước 7: Trả kết quả thống kê cấu trúc file về cho Frontend dựng giao diện
        return new PreviewRubricCriterionImportResponse(
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