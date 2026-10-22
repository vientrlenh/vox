package com.sep.vox.application.port.input.usecase.assessmentapeal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.SearchAppealsByAssessmentQuery;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.AppealSummaryInfo;
import com.sep.vox.application.projection.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.shared.PageResult;

/**
 * Đơn phúc khảo của MỘT kỳ thi, cho chủ tịch kỳ thi đó (và school admin cùng trường).
 *
 * <p>Tách khỏi {@link ViewAssessmentAppealsUseCase} thay vì nới nó: màn của school admin quét
 * toàn trường và không nhận {@code assessmentId}, nới ra là mở dữ liệu phúc khảo của cả trường
 * cho một giáo viên.
 *
 * <p>Trước đây chỉ phục vụ bài kiểm tra trên lớp ({@code classTestAppeals}). Nay nhận mọi
 * loại kỳ thi qua {@link AssessmentGradingAccessService#isAssessmentChair}, vì chủ tịch kỳ thi tập
 * trung là người bấm nút công bố kết quả mà đơn phúc khảo đang mở lại là một trong hai
 * thứ chặn nút đó — họ phải thấy được còn đơn nào và ai đang cầm.
 *
 * <p>Cố ý dừng ở DANH SÁCH, không mở {@link ViewAssessmentAppealDetailUseCase}: chi tiết đơn
 * mang theo bài làm, transcript và audio của học sinh, mà để gỡ tắc công bố thì chủ tịch
 * chỉ cần biết đơn nào còn mở, đang ở bước nào và hạn bao giờ. Quyết định duyệt/từ chối
 * đơn vẫn là của nhà trường.
 */
@Service
public class ViewAssessmentAppealsByAssessmentUseCase
        implements IUseCase<SearchAppealsByAssessmentQuery, PageResult<AppealSummaryInfo>> {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public ViewAssessmentAppealsByAssessmentUseCase(
            AssessmentAppealQueryRepository assessmentAppealQueryRepository,
            SchoolUserRepository schoolUserRepository,
            AssessmentRepository assessmentRepository,
            AssessmentAppealAccessService assessmentAppealAccessService,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentAppealQueryRepository = assessmentAppealQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentAppealAccessService = assessmentAppealAccessService;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppealSummaryInfo> execute(SearchAppealsByAssessmentQuery input) {
        var currentUserId = assessmentAppealAccessService.requireActiveUserId();
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra."));
        assessmentGradingAccessService.authorizeSchoolAdminOrAssessmentChair(
            assessment.getSchoolId(), assessment.getId(), currentUserId);

        // Trường ưu tiên lấy từ phiên đăng nhập; lùi về trường của kỳ thi khi người gọi
        // không gắn với trường nào — đó là system admin, và họ đã qua được cửa phân quyền
        // ở trên. Cùng cách xử lý với ViewAssessmentAppealDetailUseCase; ném ForbiddenException
        // ở đây sẽ chặn đúng người vừa được cho phép.
        var schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(assessment.getSchoolId());
        return assessmentAppealQueryRepository.searchAppeals(
            schoolId, input.assessmentId(), input.status(), input.keyword(), input.pageRequest().repositoryPage(), input.pageRequest().size());
    }
}
