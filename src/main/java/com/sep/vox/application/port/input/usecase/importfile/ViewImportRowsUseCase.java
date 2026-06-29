package com.sep.vox.application.port.input.usecase.importfile;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.importfile.ImportRowResponseMapper;
import com.sep.vox.application.port.input.query.ViewImportRowsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.ImportRowResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewImportRowsUseCase implements IUseCase<ViewImportRowsQuery, PageResult<ImportRowResponse>> {

    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;
    private final ImportRowResponseMapper importRowResponseMapper;
    private final SchoolUserRepository schoolUserRepository;

    public ViewImportRowsUseCase(
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            UserContextPort userContextPort,
            ImportRowResponseMapper importRowResponseMapper,
            SchoolUserRepository schoolUserRepository) {
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.userContextPort = userContextPort;
        this.importRowResponseMapper = importRowResponseMapper;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ImportRowResponse> execute(ViewImportRowsQuery input) {
        if (input == null || input.sessionId() == null) {
            throw new IllegalArgumentException("Phiên import không được để trống");
        }
        if (input.page() <= 0 || input.size() <= 0) {
            throw new IllegalArgumentException("Số trang và kích thước trang không hợp lệ");
        }
        var status = parseStatus(input.status());

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        var session = findSession(input.sessionId());
        validateSessionBelongsToSchool(session, schoolId);

        var rows = importRowRepository.findBySessionId(
            session.getId(),
            status,
            input.page(), 
            input.size()
        );
        return importRowResponseMapper.toResponsePage(rows);
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
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalStateException("Người dùng hiện tại không thuộc trường nào"));
        return schoolUser.getSchoolId();
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

    private ImportRowStatus parseStatus(String status) {
        var normalized = StringNormalization.trimAndCollapseSpaces(status);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        try {
            return ImportRowStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Trạng thái dòng import không hợp lệ");
        }
    }
}
