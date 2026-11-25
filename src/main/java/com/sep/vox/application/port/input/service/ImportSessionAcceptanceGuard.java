package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportSessionRepository;

/**
 * The checks shared by the use cases that accept a previewed import session before it is queued.
 */
@Service
@RequiredArgsConstructor
public class ImportSessionAcceptanceGuard {

    private final ImportSessionRepository importSessionRepository;

    /**
     * Requires a previewed, unexpired session of the given type that belongs to the caller. A session found
     * expired is marked {@code EXPIRED} before the failure is thrown.
     */
    public void requireAcceptableSession(
            UUID importSessionId,
            ImportType expectedType,
            String wrongTypeMessage,
            UUID currentUserId,
            Instant now) {
        ImportSession session = importSessionRepository.findById(importSessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên import với id: " + importSessionId));

        if (session.getType() != expectedType) {
            throw new IllegalArgumentException(wrongTypeMessage);
        }
        if (!session.isCreatedBy(currentUserId)) {
            throw new IllegalArgumentException("Phiên import không thuộc người dùng hiện tại");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalArgumentException("Phiên import không ở trạng thái cho accept");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            session.setStatus(ImportSessionStatus.EXPIRED);
            importSessionRepository.save(session);
            throw new IllegalArgumentException("Phiên import đã hết hạn");
        }
    }

    public void requireMappedFields(Map<String, String> confirmedMapping, Set<String> requiredFields) {
        Set<String> mappedFields = new HashSet<String>();
        confirmedMapping.values().stream()
            .filter(Objects::nonNull)
            .map(s -> s.strip())
            .forEach(mappedFields::add);
        List<String> missingFields = requiredFields.stream()
            .filter(field -> !mappedFields.contains(field))
            .toList();
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException(
                "Mapping import thiếu trường bắt buộc: " + String.join(", ", missingFields)
            );
        }
    }
}
