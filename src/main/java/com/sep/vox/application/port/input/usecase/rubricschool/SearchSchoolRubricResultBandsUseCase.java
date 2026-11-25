package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.*;
import com.sep.vox.application.port.input.query.rubricschool.SearchSchoolRubricResultBandsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricResultBandDto;
import com.sep.vox.domain.mapper.RubricResultBandDtoMapper;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchSchoolRubricResultBandsUseCase implements IUseCase<SearchSchoolRubricResultBandsQuery, PageResult<RubricResultBandDto>> {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricResultBandDto> execute(SearchSchoolRubricResultBandsQuery query) {
        requireActiveSchoolMember(query.schoolId());
        requireSchoolOwnedVersion(query);

        PageResult<RubricResultBand> pageResult = rubricResultBandRepository.searchRubricResultBands(query.versionId(), query.keyword(), query.page(), query.size());
        return new PageResult<>(
                pageResult.content()
                        .stream()
                        .map(RubricResultBandDtoMapper::toDto)
                        .toList(),
                pageResult.page(), pageResult.size(), pageResult.totalElements(), pageResult.totalPages()
        );
    }

    private void requireActiveSchoolMember(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId).orElseThrow(() -> new ForbiddenException("Không thuộc trường nào."));
        if (!schoolUser.getSchoolId().equals(schoolId))
            throw new ForbiddenException("BẢO MẬT: Xâm phạm trường học khác.");
        School school = schoolRepository.findById(schoolId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường."));
        if (!school.isActive()) throw new ForbiddenException("Trường học đang bị khóa.");
    }

    private void requireSchoolOwnedVersion(SearchSchoolRubricResultBandsQuery query) {
        RubricVersion version = rubricVersionRepository.findById(query.versionId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản."));
        Rubric rubric = rubricRepository.findById(version.getRubricId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || rubric.getSchoolId() == null || !rubric.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Thang điểm này không thuộc trường của bạn.");
        }
    }
}