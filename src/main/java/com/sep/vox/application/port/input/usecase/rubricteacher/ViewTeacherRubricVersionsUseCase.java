package com.sep.vox.application.port.input.usecase.rubricteacher;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.rubricteacher.ViewTeacherRubricVersionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricVersionDto;
import com.sep.vox.domain.mapper.RubricVersionDtoMapper;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewTeacherRubricVersionsUseCase implements IUseCase<ViewTeacherRubricVersionsQuery, PageResult<RubricVersionDto>> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricVersionDto> execute(ViewTeacherRubricVersionsQuery query) {
        SchoolUser schoolUser = loadSchoolUserOfCurrentTeacherInActiveSchool();
        requireRubricOfSchool(query.rubricId(), schoolUser.getSchoolId());

        // A teacher may only view PUBLISHED versions.
        PageResult<RubricVersion> versionPage = rubricVersionRepository.findAllByRubricIdAndStatus(
                query.rubricId(),
                RubricStatus.PUBLISHED.name(),
                query.page(),
                query.size()
        );
        return RubricVersionDtoMapper.toRubricVersionPage(versionPage);
    }

    // The teacher's school comes from SchoolUser; the client never sends a schoolId.
    private SchoolUser loadSchoolUserOfCurrentTeacherInActiveSchool() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản chưa liên kết với trường học."));
        School school = schoolRepository.findById(schoolUser.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Trường học này đang bị vô hiệu hóa.");
        }
        return schoolUser;
    }

    private void requireRubricOfSchool(UUID rubricId, UUID schoolId) {
        Rubric rubric = rubricRepository.findById(rubricId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Hành động bị từ chối: Bộ Rubric này không thuộc về trường học của bạn.");
        }
    }
}