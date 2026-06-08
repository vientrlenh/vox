package com.sep.vox.application.port.input.usecase.importfile;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.importfile.ImportSessionResponseMapper;
import com.sep.vox.application.port.input.query.ViewImportSessionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.ImportSessionSummaryResponse;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewImportSessionsUseCase implements IUseCase<ViewImportSessionsQuery, PageResult<ImportSessionSummaryResponse>> {

    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;
    private final ImportSessionResponseMapper importSessionResponseMapper;

    public ViewImportSessionsUseCase(
            ImportSessionRepository importSessionRepository,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            UserContextPort userContextPort,
            ImportSessionResponseMapper importSessionResponseMapper) {
        this.importSessionRepository = importSessionRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.userContextPort = userContextPort;
        this.importSessionResponseMapper = importSessionResponseMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ImportSessionSummaryResponse> execute(ViewImportSessionsQuery input) {
        if (input == null || input.page() <= 0 || input.size() <= 0) {
            throw new IllegalArgumentException("Số trang và kích thước trang không hợp lệ");
        }
        var type = parseType(input.type());
        var status = parseStatus(input.status());

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        var result = importSessionRepository.findBySchoolId(
            schoolId,
            type,
            status,
            new PageRequest(input.page(), input.size())
        );
        return importSessionResponseMapper.toSummaryPage(result);
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
        var schoolId = currentUser.getSchoolId();
        if (schoolId == null) {
            throw new IllegalStateException("Người dùng hiện tại không thuộc trường nào");
        }
        return schoolId;
    }

    private void validateSchool(UUID schoolId) {
        var school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học không hoạt động");
        }
    }

    private ImportType parseType(String type) {
        var normalized = StringNormalization.trimAndCollapseSpaces(type);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        try {
            return ImportType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Loại import không hợp lệ");
        }
    }

    private ImportSessionStatus parseStatus(String status) {
        var normalized = StringNormalization.trimAndCollapseSpaces(status);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        try {
            return ImportSessionStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Trạng thái import không hợp lệ");
        }
    }
}
