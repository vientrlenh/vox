package com.sep.vox.application.port.input.usecase.schoolclass;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.model.languagelevel.LevelStatus;
import com.sep.vox.domain.model.schoolclass.SchoolClassStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolLevelRepository;
import com.sep.vox.domain.repository.SchoolLevelVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class UpdateSchoolClassUseCase implements IUseCase<UpdateSchoolClassCommand, SchoolClassDto> {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolLevelRepository schoolLevelRepository;
    private final SchoolLevelVersionRepository schoolLevelVersionRepository;
    private final UserContextPort userContextPort;

    public UpdateSchoolClassUseCase(
            SchoolClassRepository schoolClassRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            SchoolLevelRepository schoolLevelRepository,
            SchoolLevelVersionRepository schoolLevelVersionRepository,
            UserContextPort userContextPort) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.schoolLevelRepository = schoolLevelRepository;
        this.schoolLevelVersionRepository = schoolLevelVersionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SchoolClassDto execute(UpdateSchoolClassCommand input) {
        var command = normalize(input);
        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateSchool(schoolId);

        var schoolClass = schoolClassRepository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));
        if (!Objects.equals(schoolClass.getSchoolId(), schoolId)) {
            throw new NotFoundException("Không tìm thấy lớp học");
        }

        var status = parseStatus(command.status());
        validateTargetSchoolLevelVersion(
            command.targetSchoolLevelVersionId(),
            schoolId,
            schoolClass.getLanguageId()
        );

        schoolClass.setName(command.name());
        schoolClass.setDescription(command.description());
        schoolClass.setTargetSchoolLevelVersionId(command.targetSchoolLevelVersionId());
        schoolClass.setStatus(status);
        schoolClass.setUpdatedAt(now);
        schoolClass.setUpdatedBy(currentUserId);

        var saved = schoolClassRepository.save(schoolClass);
        return SchoolClassDtoMapper.toDto(saved);
    }

    private UpdateSchoolClassCommand normalize(UpdateSchoolClassCommand input) {
        return new UpdateSchoolClassCommand(
            input.id(),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.targetSchoolLevelVersionId(),
            StringNormalization.trimAndCollapseSpaces(input.status())
        );
    }

    private User findCurrentUser(UUID currentUserId) {
        return userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
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

    private SchoolClassStatus parseStatus(String status) {
        try {
            return SchoolClassStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Trạng thái lớp học không hợp lệ");
        }
    }

    private void validateTargetSchoolLevelVersion(UUID targetSchoolLevelVersionId, UUID schoolId, UUID languageId) {
        var levelVersion = schoolLevelVersionRepository.findById(targetSchoolLevelVersionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản cấp độ mục tiêu"));
        if (levelVersion.getStatus() != LevelStatus.PUBLISHED) {
            throw new IllegalStateException("Phiên bản cấp độ mục tiêu chưa được công bố");
        }

        var schoolLevel = schoolLevelRepository.findById(levelVersion.getSchoolLevelId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy cấp độ trường học"));
        if (!Objects.equals(schoolLevel.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Cấp độ mục tiêu không thuộc trường hiện tại");
        }
        if (!Objects.equals(schoolLevel.getLanguageId(), languageId)) {
            throw new IllegalArgumentException("Cấp độ mục tiêu không thuộc ngôn ngữ của lớp học");
        }
    }
}
