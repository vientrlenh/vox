package com.sep.vox.application.port.input.usecase.importfile;

import com.sep.vox.domain.model.school.School;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.RejectImportSessionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.RejectImportSessionResponse;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class RejectImportSessionUseCase implements IUseCase<RejectImportSessionCommand, RejectImportSessionResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public RejectImportSessionResponse execute(RejectImportSessionCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Phiên import không được để trống");
        }

        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        UUID schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        ImportSession session = findSession(input.importSessionId());
        validateSession(session, schoolId, now, currentUserId);

        String reason = normalizeReason(input.reason());
        session.setStatus(ImportSessionStatus.CANCELLED);
        session.setFailureReason(reason);
        ImportSession savedSession = importSessionRepository.save(session);

        return new RejectImportSessionResponse(
            savedSession.getId(),
            savedSession.getStatus().name(),
            savedSession.getFailureReason()
        );
    }

    private User findCurrentUser(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Người dùng hiện tại không hoạt động");
        }
        return user;
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Người dùng hiện tại không thuộc trường nào"));
        return schoolUser.getSchoolId();
    }

    private void validateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalArgumentException("Trường học không hoạt động");
        }
    }

    private ImportSession findSession(UUID importSessionId) {
        return importSessionRepository.findById(importSessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên import"));
    }

    private void validateSession(ImportSession session, UUID schoolId, Instant now, UUID currentUserId) {
        if (!Objects.equals(session.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Phiên import không thuộc trường hiện tại");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            session.setStatus(ImportSessionStatus.EXPIRED);
            importSessionRepository.save(session);
            throw new IllegalArgumentException("Phiên import đã hết hạn");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalArgumentException("Phiên import không ở trạng thái cho reject");
        }
    }

    private String normalizeReason(String reason) {
        String normalized = StringNormalization.trimAndCollapseSpaces(reason);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
