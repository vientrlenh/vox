package com.sep.vox.application.port.input.usecase.importfile;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.importfile.ImportSessionResponseMapper;
import com.sep.vox.application.port.input.query.ViewImportSessionQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.ImportSessionDetailsResponse;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewImportSessionUseCase implements IUseCase<ViewImportSessionQuery, ImportSessionDetailsResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;
    private final ImportSessionResponseMapper importSessionResponseMapper;
    private final SchoolUserRepository schoolUserRepository;

    public ViewImportSessionUseCase(
            ImportSessionRepository importSessionRepository,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            UserContextPort userContextPort,
            ImportSessionResponseMapper importSessionResponseMapper,
            SchoolUserRepository schoolUserRepository) {
        this.importSessionRepository = importSessionRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.userContextPort = userContextPort;
        this.importSessionResponseMapper = importSessionResponseMapper;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ImportSessionDetailsResponse execute(ViewImportSessionQuery input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Phiên import không được để trống");
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        var session = findSession(input.importSessionId());
        validateSessionBelongsToSchool(session, schoolId);
        return importSessionResponseMapper.toDetails(session);
    }

    private User findCurrentUser(UUID currentUserId) {
        var user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Người dùng hiện tại không hoạt động");
        }
        return user;
    }

    private UUID getSchoolId(User currentUser) {
        return schoolUserRepository.findByUserId(currentUser.getId())
            .map(SchoolUser::getSchoolId)
            .orElseThrow(() -> new IllegalStateException("Người dùng hiện tại không thuộc trường nào"));
    }

    private void validateSchool(UUID schoolId) {
        var school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học không hoạt động");
        }
    }

    private ImportSession findSession(UUID importSessionId) {
        return importSessionRepository.findById(importSessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên import"));
    }

    private void validateSessionBelongsToSchool(ImportSession session, UUID schoolId) {
        if (!Objects.equals(session.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Phiên import không thuộc trường hiện tại");
        }
    }
}
