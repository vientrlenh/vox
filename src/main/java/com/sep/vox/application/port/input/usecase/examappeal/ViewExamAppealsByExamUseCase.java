package com.sep.vox.application.port.input.usecase.examappeal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.SearchAppealsByExamQuery;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.repository.ExamAppealQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Đơn phúc khảo của MỘT kỳ thi, cho chủ tịch kỳ thi đó (và school admin cùng trường).
 *
 * <p>Tách khỏi {@link ViewExamAppealsUseCase} thay vì nới nó: màn của school admin quét
 * toàn trường và không nhận {@code examId}, nới ra là mở dữ liệu phúc khảo của cả trường
 * cho một giáo viên.
 *
 * <p>Trước đây chỉ phục vụ bài kiểm tra trên lớp ({@code classTestAppeals}). Nay nhận mọi
 * loại kỳ thi qua {@link ExamGradingAccessService#isExamChair}, vì chủ tịch kỳ thi tập
 * trung là người bấm nút công bố kết quả mà đơn phúc khảo đang mở lại là một trong hai
 * thứ chặn nút đó — họ phải thấy được còn đơn nào và ai đang cầm.
 *
 * <p>Cố ý dừng ở DANH SÁCH, không mở {@link ViewExamAppealDetailUseCase}: chi tiết đơn
 * mang theo bài làm, transcript và audio của học sinh, mà để gỡ tắc công bố thì chủ tịch
 * chỉ cần biết đơn nào còn mở, đang ở bước nào và hạn bao giờ. Quyết định duyệt/từ chối
 * đơn vẫn là của nhà trường.
 */
@Service
public class ViewExamAppealsByExamUseCase
        implements IUseCase<SearchAppealsByExamQuery, PageResult<AppealSummaryInfo>> {

    private final ExamAppealQueryRepository examAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final ExamRepository examRepository;
    private final ExamAppealAccessService examAppealAccessService;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewExamAppealsByExamUseCase(
            ExamAppealQueryRepository examAppealQueryRepository,
            SchoolUserRepository schoolUserRepository,
            ExamRepository examRepository,
            ExamAppealAccessService examAppealAccessService,
            ExamGradingAccessService examGradingAccessService) {
        this.examAppealQueryRepository = examAppealQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.examRepository = examRepository;
        this.examAppealAccessService = examAppealAccessService;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppealSummaryInfo> execute(SearchAppealsByExamQuery input) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra."));
        examGradingAccessService.authorizeSchoolAdminOrExamChair(
            exam.getSchoolId(), exam.getId(), currentUserId);

        // Trường ưu tiên lấy từ phiên đăng nhập; lùi về trường của kỳ thi khi người gọi
        // không gắn với trường nào — đó là system admin, và họ đã qua được cửa phân quyền
        // ở trên. Cùng cách xử lý với ViewExamAppealDetailUseCase; ném ForbiddenException
        // ở đây sẽ chặn đúng người vừa được cho phép.
        var schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(exam.getSchoolId());
        return examAppealQueryRepository.searchAppeals(
            schoolId, input.examId(), input.status(), input.keyword(), input.page(), input.size());
    }
}
