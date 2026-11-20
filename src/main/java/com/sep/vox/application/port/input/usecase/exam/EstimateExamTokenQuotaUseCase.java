package com.sep.vox.application.port.input.usecase.exam;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.EstimateExamTokenQuotaQuery;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.exam.ExamTokenEstimateResponse;
import com.sep.vox.domain.repository.ExamRepository;

/**
 * Cảnh báo ước lượng chi phí AI NGAY LÚC TẠO bài thi (trước publish) -- không throw, chỉ để FE
 * hiện banner cảnh báo sớm. Việc CHẶN thật vẫn nằm ở ClassTestTokenQuotaGuardService.requireWithinTokenQuota,
 * gọi lúc publish/sửa/thêm thí sinh.
 */
@Service
public class EstimateExamTokenQuotaUseCase implements IUseCase<EstimateExamTokenQuotaQuery, ExamTokenEstimateResponse> {

    private final ExamRepository examRepository;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private final UserContextPort userContextPort;

    public EstimateExamTokenQuotaUseCase(
            ExamRepository examRepository,
            ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.classTestTokenQuotaGuardService = classTestTokenQuotaGuardService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamTokenEstimateResponse execute(EstimateExamTokenQuotaQuery input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        if (!userContextPort.isSystemAdmin() && !exam.getSchoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        return classTestTokenQuotaGuardService.estimateTokenQuota(exam);
    }
}
