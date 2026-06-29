package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.SearchSchoolRubricCriterionBandsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricCriterionBandDto;
import com.sep.vox.domain.mapper.RubricCriterionBandDtoMapper;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SearchSchoolRubricCriterionBandsUseCase implements IUseCase<SearchSchoolRubricCriterionBandsQuery, PageResult<RubricCriterionBandDto>> {

    private final RubricCriterionBandRepository rubricCriterionBandRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;

    public SearchSchoolRubricCriterionBandsUseCase(
            RubricCriterionBandRepository rubricCriterionBandRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolRepository schoolRepository) {
        this.rubricCriterionBandRepository = rubricCriterionBandRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolRepository = schoolRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricCriterionBandDto> execute(SearchSchoolRubricCriterionBandsQuery query) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản đã bị khóa.");

        var schoolUser = schoolUserRepository.findByUserId(currentUserId).orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));
        if (!schoolUser.getSchoolId().equals(query.schoolId()))
            throw new ForbiddenException("BẢO MẬT: Không được can thiệp trường khác.");

        var school = schoolRepository.findById(query.schoolId()).orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) throw new ForbiddenException("Hành động bị từ chối: Trường học bị khóa.");

        var criterion = rubricCriterionRepository.findById(query.criterionId()).orElseThrow(() -> new NotFoundException("Không tìm thấy tiêu chí chấm điểm."));
        var version = rubricVersionRepository.findById(criterion.getRubricVersionId()).orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));
        var rubric = rubricRepository.findById(version.getRubricId()).orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || rubric.getSchoolId() == null || !rubric.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Tiêu chí này không thuộc quyền sở hữu của trường bạn.");
        }

        var pageResult = rubricCriterionBandRepository.searchRubricCriterionBands(query.criterionId(), query.keyword(), query.page(), query.size());
        return new PageResult<>(
                pageResult.content().stream()
                        .map(RubricCriterionBandDtoMapper::toDto)
                        .toList(),
                pageResult.page(), pageResult.size(), pageResult.totalElements(), pageResult.totalPages()
        );
    }
}