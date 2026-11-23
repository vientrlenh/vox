package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.PreviewSchoolAssessmentPolicyImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewAssessmentPolicyImportResponse;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PreviewSchoolAssessmentPolicyImportFromFileUseCase implements IUseCase<PreviewSchoolAssessmentPolicyImportFromFileCommand, PreviewAssessmentPolicyImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final FileProcessingPort fileProcessingPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public PreviewAssessmentPolicyImportResponse execute(PreviewSchoolAssessmentPolicyImportFromFileCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được phân bổ vào trường học nào."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không được quyền nạp file cho một trường học khác.");
        }

        School school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }

        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.ASSESSMENT_POLICY);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("File tải lên trống hoặc không chứa dữ liệu.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS);

        ImportSession session = ImportSession.builder()
            .schoolId(command.schoolId())
            .type(ImportType.ASSESSMENT_POLICY)
            .fileName(command.file().fileName())
            .originalHeadersJson(jsonSerializationPort.toJson(parsedResult.originalHeaders()))
            .suggestedMappingJson(jsonSerializationPort.toJson(parsedResult.suggestedMapping()))
            .confirmedMappingJson(null)
            .validRows(0)
            .invalidRows(0)
            .importedRows(0)
            .skippedRows(0)
            .totalRows(parsedResult.rows().size())
            .failureReason(null)
            .status(ImportSessionStatus.PREVIEWED)
            .importedEntityId(null)
            .expiresAt(expiresAt)
            .claimedAt(null)
            .claimedBy(null)
            .leaseExpiresAt(null)
            .attempts(0)
            .build();

        ImportSession savedSession = importSessionRepository.save(session);
        UUID generatedSessionId = savedSession.getId();

        List<ImportRow> importRows = IntStream.range(0, parsedResult.rows().size())
                .mapToObj(i -> ImportRow.builder()
                    .sessionId(generatedSessionId)
                    .rowNumber(i + 1)
                    .rawDataJson(jsonSerializationPort.toJson(parsedResult.rows().get(i)))
                    .mappedDataJson(null)
                    .errorsJson(null)
                    .status(ImportRowStatus.PENDING)
                    .build()).toList();

        importRowRepository.saveAll(importRows);

        return new PreviewAssessmentPolicyImportResponse(
                generatedSessionId, command.file().fileName(), parsedResult.originalHeaders(),
                parsedResult.suggestedMapping(), parsedResult.sampleRows(), parsedResult.totalRows(), expiresAt
        );
    }
}
