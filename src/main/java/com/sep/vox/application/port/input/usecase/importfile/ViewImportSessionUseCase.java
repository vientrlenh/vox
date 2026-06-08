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
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewImportSessionUseCase implements IUseCase<ViewImportSessionQuery, ImportSessionDetailsResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;

    public ViewImportSessionUseCase(
            ImportSessionRepository importSessionRepository,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            UserContextPort userContextPort) {
        this.importSessionRepository = importSessionRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ImportSessionDetailsResponse execute(ViewImportSessionQuery input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Phien import khong duoc de trong");
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        var session = findSession(input.importSessionId());
        validateSessionBelongsToSchool(session, schoolId);
        return ImportSessionResponseMapper.toDetails(session);
    }

    private User findCurrentUser(UUID currentUserId) {
        var user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Khong tim thay nguoi dung hien tai"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Nguoi dung hien tai khong hoat dong");
        }
        return user;
    }

    private UUID getSchoolId(User currentUser) {
        var schoolId = currentUser.getSchoolId();
        if (schoolId == null) {
            throw new IllegalStateException("Nguoi dung hien tai khong thuoc truong nao");
        }
        return schoolId;
    }

    private void validateSchool(UUID schoolId) {
        var school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new NotFoundException("Khong tim thay truong hoc"));
        if (!school.isActive()) {
            throw new IllegalStateException("Truong hoc khong hoat dong");
        }
    }

    private ImportSession findSession(UUID importSessionId) {
        return importSessionRepository.findById(importSessionId)
            .orElseThrow(() -> new NotFoundException("Khong tim thay phien import"));
    }

    private void validateSessionBelongsToSchool(ImportSession session, UUID schoolId) {
        if (!Objects.equals(session.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Phien import khong thuoc truong hien tai");
        }
    }
}
