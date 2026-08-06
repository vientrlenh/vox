package com.sep.vox.application.port.input.usecase.framework;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.AcceptFrameworkVersionImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptFrameworkVersionImportResponse;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AcceptFrameworkVersionImportUseCase
        implements IUseCase<AcceptFrameworkVersionImportCommand, AcceptFrameworkVersionImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final FrameworkRepository frameworkRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    public AcceptFrameworkVersionImportUseCase(
            ImportSessionRepository importSessionRepository,
            FrameworkRepository frameworkRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            JsonSerializationPort jsonSerializationPort) {
        this.importSessionRepository = importSessionRepository;
        this.frameworkRepository = frameworkRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public AcceptFrameworkVersionImportResponse execute(AcceptFrameworkVersionImportCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản không hoạt động.");
        }

        ImportSession session = importSessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên import."));
        if (session.getType() != ImportType.FRAMEWORK_VERSION) {
            throw new IllegalArgumentException("Phiên import không đúng loại.");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalStateException("Chỉ có thể xác nhận phiên đang ở trạng thái PREVIEWED.");
        }

        frameworkRepository.findById(session.getImportedEntityId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khung năng lực."));

        if (command.confirmedMapping() != null) {
            session.setConfirmedMappingJson(jsonSerializationPort.toJson(command.confirmedMapping()));
        }

        session.setStatus(ImportSessionStatus.QUEUED);
        session.setAttempts(0);
        session.setUpdatedAt(Instant.now());
        session.setUpdatedBy(currentUserId);
        importSessionRepository.save(session);

        return new AcceptFrameworkVersionImportResponse(
                session.getId(), session.getTotalRows(), 0, 0, 0, session.getStatus().name()
        );
    }
}
