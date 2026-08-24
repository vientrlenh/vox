package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSystemAssessmentPoliciesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.AssessmentPolicyDto;
import com.sep.vox.domain.mapper.AssessmentPolicyDtoMapper;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewSystemAssessmentPoliciesUseCase implements IUseCase<ViewSystemAssessmentPoliciesQuery, PageResult<AssessmentPolicyDto>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ViewSystemAssessmentPoliciesUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssessmentPolicyDto> execute(ViewSystemAssessmentPoliciesQuery query) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }

        String safeStatus = null;
        if (query.status() != null && !query.status().isBlank()) {
            try {
                safeStatus = AssessmentPolicyStatus.valueOf(query.status().trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái (status) không hợp lệ. Chỉ chấp nhận DRAFT, PUBLISHED, ARCHIVED.");
            }
        }

        if (query.effectiveFrom() != null && query.effectiveTo() != null && query.effectiveFrom().isAfter(query.effectiveTo())) {
            throw new IllegalArgumentException("Khoảng thời gian effectiveFrom/effectiveTo không hợp lệ.");
        }

        // School Admin xem danh sách này để chọn mẫu đem về sao chép, nên chỉ được thấy bản ĐÃ BAN
        // HÀNH: bản nháp của hệ thống là việc đang làm dở, chưa rà soát. Cùng lằn ranh mà
        // CloneSystemRubricToSchoolUseCase áp cho rubric mẫu -- ở đó chặn lúc sao, ở đây giấu ngay
        // từ lúc liệt kê để trường không chọn phải thứ rồi sẽ bị từ chối.
        if (!userContextPort.isSystemAdmin()) {
            if (safeStatus != null && !AssessmentPolicyStatus.PUBLISHED.name().equals(safeStatus)) {
                throw new IllegalArgumentException(
                        "Chỉ xem được các chính sách mẫu đã ban hành (PUBLISHED).");
            }
            safeStatus = AssessmentPolicyStatus.PUBLISHED.name();
        }

        var policyPage = assessmentPolicyRepository.findAllSystemWide(safeStatus, query.languageId(), query.rubricVersionId(),
                query.effectiveFrom(), query.effectiveTo(), query.page(), query.size());
        return AssessmentPolicyDtoMapper.toDtoPage(policyPage);
    }
}
