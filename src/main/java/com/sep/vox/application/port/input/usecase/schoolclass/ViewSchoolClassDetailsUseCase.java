package com.sep.vox.application.port.input.usecase.schoolclass;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.schoolclass.ViewSchoolClassDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewSchoolClassDetailsUseCase implements IUseCase<ViewSchoolClassDetailsQuery, SchoolClassDto> {

    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional(readOnly = true)
    public SchoolClassDto execute(ViewSchoolClassDetailsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        UUID schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        SchoolClass schoolClass = schoolClassRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        if (!Objects.equals(schoolClass.getSchoolId(), schoolId)) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học");
        }
        return SchoolClassDtoMapper.toDto(schoolClass);
    }

    private User findCurrentUser(UUID currentUserId) {
        User user = userRepository.findByIdAndStatus(currentUserId, UserStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));
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
