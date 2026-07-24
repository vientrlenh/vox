package com.sep.vox.application.port.input.usecase.schooluser;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AcceptSchoolUserImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class AcceptSchoolUserImportUseCase
        implements IUseCase<AcceptSchoolUserImportCommand, Void> {

    // startDate/endDate chỉ bắt buộc với học sinh nên được kiểm tra ở mức từng dòng
    // (SchoolUserImportCommitHandler), không ép buộc trong mapping cấp file.
    private static final Set<String> REQUIRED_FIELDS = Set.of("email", "fullName", "roleCode", "phone",
            "dateOfBirth", "address");
    private static final String USER_TYPE = "USER";

    private final ImportSessionRepository importSessionRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;

    public AcceptSchoolUserImportUseCase(
            ImportSessionRepository importSessionRepository,
            UserContextPort userContextPort,
            JsonSerializationPort jsonSerializationPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolRepository schoolRepository) {
        this.importSessionRepository = importSessionRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolRepository = schoolRepository;
    }

    @Override
    @Transactional
    public Void execute(AcceptSchoolUserImportCommand input) {
        validateCommand(input);

        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateRequestedSchool(input.schoolId(), schoolId);
        var school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học không hoạt động");
        }

        var session = findSession(input.importSessionId());
        validateSession(session, schoolId, now);
        validateRequiredMapping(input.confirmedMapping());

        var confirmedMappingJson = jsonSerializationPort.toJson(input.confirmedMapping());
        var queued = importSessionRepository.markQueued(
                input.importSessionId(), USER_TYPE, confirmedMappingJson, now, currentUserId);
        if (queued == 0) {
            throw new IllegalStateException("Phiên import không ở trạng thái cho accept hoặc đã hết hạn");
        }
        return null;
    }

    private void validateCommand(AcceptSchoolUserImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Phiên import không được để trống");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("Mapping import không được để trống");
        }
    }

    private User findCurrentUser(UUID currentUserId) {
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalStateException("Người dùng hiện tại không thuộc trường nào"));
        return schoolUser.getSchoolId();
    }

    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (requestedSchoolId == null) {
            throw new IllegalArgumentException("Trường học không được để trống");
        }
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("Trường học không khớp với người dùng hiện tại");
        }
    }

    private ImportSession findSession(UUID importSessionId) {
        return importSessionRepository.findById(importSessionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên import"));
    }

    private void validateSession(ImportSession session, UUID schoolId, OffsetDateTime now) {
        if (session.getType() != ImportType.USER) {
            throw new IllegalArgumentException("Phiên import không phải là import người dùng");
        }
        if (!Objects.equals(session.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Phiên import không thuộc trường hiện tại");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalStateException("Phiên import không ở trạng thái cho accept");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            session.setStatus(ImportSessionStatus.EXPIRED);
            importSessionRepository.save(session);
            throw new IllegalStateException("Phiên import đã hết hạn");
        }
    }

    private void validateRequiredMapping(Map<String, String> confirmedMapping) {
        var mappedFields = new HashSet<String>();
        confirmedMapping.values().stream()
                .filter(value -> value != null)
                .map(value -> value.strip())
                .forEach(mappedFields::add);
        var missingFields = REQUIRED_FIELDS.stream()
                .filter(field -> !mappedFields.contains(field))
                .toList();
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "Mapping import thiếu trường bắt buộc: " + String.join(", ", missingFields));
        }
    }
}
