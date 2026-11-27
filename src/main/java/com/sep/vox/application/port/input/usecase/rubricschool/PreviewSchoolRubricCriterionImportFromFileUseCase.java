package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream; // Sử dụng Command dùng chung của bác

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.PreviewRubricCriterionImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewRubricCriterionImportResponse;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportSession;
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
@RequiredArgsConstructor
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

    @Override
    @Transactional
    public PreviewRubricCriterionImportResponse execute(PreviewRubricCriterionImportCommand command) {
        if (command.schoolId() == null) {
            throw new IllegalArgumentException("Yêu cầu không hợp lệ: Thiếu mã trường học đối với luồng School Admin.");
        }

        requireActiveSchoolMember(command.schoolId());
        requireSchoolOwnedRubricVersion(command);

        ParseImportFileResult parsedResult = parseNonEmptyFile(command);
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);

        // Store the school id strictly so the background worker can pick up the job.
        UUID generatedSessionId = saveSessionWithRows(command.schoolId(), ImportType.RUBRIC_CRITERION,
                command.file().fileName(), command.rubricVersionId(), parsedResult, expiresAt);

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

    private ParseImportFileResult parseNonEmptyFile(PreviewRubricCriterionImportCommand command) {
        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.RUBRIC_CRITERION);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("File tải lên rỗng hoặc không có dữ liệu hợp lệ.");
        }
        return parsedResult;
    }

    private void requireActiveSchoolMember(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Bạn không thuộc trường học nào để thực hiện chức năng này."));

        // Security: the school id in the URL must match the caller's real school.
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên dữ liệu của trường khác.");
        }
    }

    private void requireSchoolOwnedRubricVersion(PreviewRubricCriterionImportCommand command) {
        RubricVersion version = rubricVersionRepository.findById(command.rubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phiên bản (Version) yêu cầu."));
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));

        // Security: the id of a SYSTEM rubric or of another school's rubric must not be accepted through this school's URL.
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Bảo mật: Bộ Rubric này không thuộc sở hữu của trường bạn.");
        }
    }

    private UUID saveSessionWithRows(UUID schoolId, ImportType importType, String fileName, UUID importedEntityId,
                                     ParseImportFileResult parsedResult, Instant expiresAt) {
        ImportSession session = ImportSession.previewedForEntity(
            schoolId,
            importType,
            fileName,
            jsonSerializationPort.toJson(parsedResult.originalHeaders()),
            jsonSerializationPort.toJson(parsedResult.suggestedMapping()),
            parsedResult.rows().size(),
            expiresAt,
            importedEntityId);
        ImportSession savedSession = importSessionRepository.save(session);
        UUID generatedSessionId = savedSession.getId();

        List<ImportRow> importRows = IntStream.range(0, parsedResult.rows().size())
                .mapToObj(i -> ImportRow.pending(
                    generatedSessionId,
                    i + 1,
                    jsonSerializationPort.toJson(parsedResult.rows().get(i)))).toList();
        importRowRepository.saveAll(importRows);
        return generatedSessionId;
    }
}