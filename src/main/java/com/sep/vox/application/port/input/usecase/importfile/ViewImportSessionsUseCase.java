package com.sep.vox.application.port.input.usecase.importfile;

import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.school.School;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.importfile.ImportSessionResponseMapper;
import com.sep.vox.application.port.input.query.ViewImportSessionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.ImportSessionSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewImportSessionsUseCase implements IUseCase<ViewImportSessionsQuery, PageResult<ImportSessionSummaryResponse>> {

    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;
    private final ImportSessionResponseMapper importSessionResponseMapper;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ImportSessionSummaryResponse> execute(ViewImportSessionsQuery input) {
        if (input == null || input.page() <= 0 || input.size() <= 0) {
            throw new IllegalArgumentException("Số trang và kích thước trang không hợp lệ");
        }
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        UUID schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        PageResult<ImportSession> result = importSessionRepository.findBySchoolId(
            schoolId,
            input.type(),
            input.status(),
            input.page(), 
            input.size()
        );
        return importSessionResponseMapper.toSummaryPage(result);
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
}
