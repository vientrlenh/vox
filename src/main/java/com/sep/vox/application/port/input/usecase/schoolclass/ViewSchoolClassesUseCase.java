package com.sep.vox.application.port.input.usecase.schoolclass;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewSchoolClassesUseCase implements IUseCase<ViewSchoolClassesQuery, PageResult<SchoolClassDto>> {

    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolClassDto> execute(ViewSchoolClassesQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        UUID schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        PageResult<SchoolClass> result = schoolClassRepository.findBySchoolId(
            schoolId,
            StringNormalization.trimAndCollapseSpaces(input.search()),
            input.status(),
            input.languageId(),
            input.schoolGradeId(),
            input.page(), 
            input.size()
        );
        return SchoolClassDtoMapper.toDtoPage(result);
    }

    private User findCurrentUser(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
        validateCurrentUserIsActive(user);
        return user;
    }

    private void validateCurrentUserIsActive(User currentUser) {
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Người dùng hiện tại không hoạt động");
        }
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalStateException("Người dùng hiện tại không thuộc trường nào"));
        return schoolUser.getSchoolId();
    }

    private void validateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học không hoạt động");
        }
    }
}
