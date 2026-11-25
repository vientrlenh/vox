package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.AcceptRubricResultBandImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptRubricResultBandImportResponse;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcceptSchoolRubricResultBandImportUseCase implements IUseCase<AcceptRubricResultBandImportCommand, AcceptRubricResultBandImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public AcceptRubricResultBandImportResponse execute(AcceptRubricResultBandImportCommand command) {
        if (command.schoolId() == null) {
            throw new IllegalArgumentException("Yêu cầu không hợp lệ: Thiếu mã trường học.");
        }

        requireActiveSchoolMember(command.schoolId());
        ImportSession session = loadPreviewedSessionOfSchool(command);
        requireSchoolOwnedRubric(session, command.schoolId());

        if (command.confirmedMapping() != null) {
            String mappingJson = jsonSerializationPort.toJson(command.confirmedMapping());
            session.setConfirmedMappingJson(mappingJson);
        }

        session.setStatus(ImportSessionStatus.QUEUED);
        session.setAttempts(0);
        importSessionRepository.save(session);

        return new AcceptRubricResultBandImportResponse(session.getId(), session.getTotalRows(), 0, 0, 0, session.getStatus().name());
    }

    private void requireActiveSchoolMember(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId).orElseThrow(() -> new ForbiddenException("Không thuộc trường học nào."));
        if (!schoolUser.getSchoolId().equals(schoolId)) throw new ForbiddenException("BẢO MẬT: Mã bảo mật trường học không trùng khớp.");
    }

    private ImportSession loadPreviewedSessionOfSchool(AcceptRubricResultBandImportCommand command) {
        ImportSession session = importSessionRepository.findById(command.sessionId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên làm việc Import."));

        // Cross-check that the session belongs to the caller's school.
        if (session.getType() != ImportType.RUBRIC_RESULT_BAND || session.getImportedEntityId() == null
                || session.getSchoolId() == null || !session.getSchoolId().equals(command.schoolId())) {
            throw new IllegalArgumentException("Dữ liệu phiên làm việc không hợp lệ hoặc không thuộc trường của bạn.");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalArgumentException("Phiên làm việc phải ở trạng thái PREVIEWED mới có thể thực hiện Xác nhận.");
        }
        return session;
    }

    private void requireSchoolOwnedRubric(ImportSession session, UUID schoolId) {
        RubricVersion version = rubricVersionRepository.findById(session.getImportedEntityId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phiên bản Rubric tương ứng."));
        Rubric rubric = rubricRepository.findById(version.getRubricId()).orElseThrow();

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Bảo mật: Bộ Rubric này không thuộc sở hữu của trường học bạn.");
        }
    }
}