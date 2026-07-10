package com.sep.vox.application.port.input.usecase.scoringrule;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.SearchSchoolScoringRuleQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.ScoringRuleDto;
import com.sep.vox.domain.mapper.ScoringRuleDtoMapper;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ScoringRuleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class SearchSchoolScoringRuleUseCase implements IUseCase<SearchSchoolScoringRuleQuery, PageResult<ScoringRuleDto>> {

    private final ScoringRuleRepository scoringRuleRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public SearchSchoolScoringRuleUseCase(
            ScoringRuleRepository scoringRuleRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.scoringRuleRepository = scoringRuleRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ScoringRuleDto> execute(SearchSchoolScoringRuleQuery query) {
        // 1. Kiểm tra tài khoản School Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Kiểm tra tài khoản thuộc đúng trường học yêu cầu
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được phân bổ vào trường học nào."));
        if (!schoolUser.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền xem Scoring Rule của trường khác.");
        }

        // 3. Kiểm tra Assessment Policy tồn tại và thuộc đúng trường học
        var policy = assessmentPolicyRepository.findById(query.policyId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền xem Scoring Rule của trường khác.");
        }

        // Lưu ý: không chặn theo status Policy - cho phép xem lại rule kể cả khi Policy đã PUBLISHED/ARCHIVED

        // 4. Tìm kiếm/phân trang
        var pageResult = scoringRuleRepository.searchByPolicyId(
                query.policyId(), query.keyword(), query.isActive(), query.page(), query.size());

        return new PageResult<>(
                pageResult.content().stream().map(ScoringRuleDtoMapper::toDto).toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
    }
}