package com.sep.vox.application.port.input.usecase.assessment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.EstimateAssessmentTokenQuotaQuery;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.assessment.AssessmentTokenEstimateResponse;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

/**
 * Cảnh báo ước lượng chi phí AI NGAY LÚC TẠO bài thi (trước publish) -- không throw, chỉ để FE
 * hiện banner cảnh báo sớm. Việc CHẶN thật vẫn nằm ở ClassTestTokenQuotaGuardService.requireWithinTokenQuota,
 * gọi lúc publish/sửa/thêm thí sinh.
 */
@Service
public class EstimateAssessmentTokenQuotaUseCase implements IUseCase<EstimateAssessmentTokenQuotaQuery, AssessmentTokenEstimateResponse> {

    private final AssessmentRepository assessmentRepository;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private final UserContextPort userContextPort;

    public EstimateAssessmentTokenQuotaUseCase(
            AssessmentRepository assessmentRepository,
            ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.classTestTokenQuotaGuardService = classTestTokenQuotaGuardService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentTokenEstimateResponse execute(EstimateAssessmentTokenQuotaQuery input) {
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        if (!userContextPort.isSystemAdmin() && !assessment.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        return classTestTokenQuotaGuardService.estimateTokenQuota(assessment);
    }
}
