package com.sep.vox.application.port.input.usecase.examappeal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.SearchClassTestAppealsQuery;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.repository.ExamAppealQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Đơn phúc khảo của MỘT bài kiểm tra trên lớp, cho chính giáo viên tạo bài.
 *
 * <p>Tách khỏi {@link ViewExamAppealsUseCase} thay vì nới nó: màn của school admin
 * quét toàn trường và không nhận {@code examId}, nới ra là mở dữ liệu phúc khảo của
 * cả trường cho một giáo viên.
 */
@Service
public class ViewClassTestAppealsUseCase
        implements IUseCase<SearchClassTestAppealsQuery, PageResult<AppealSummaryInfo>> {

    private final ExamAppealQueryRepository examAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final ExamAppealAccessService examAppealAccessService;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewClassTestAppealsUseCase(
            ExamAppealQueryRepository examAppealQueryRepository,
            SchoolUserRepository schoolUserRepository,
            ExamAppealAccessService examAppealAccessService,
            ExamGradingAccessService examGradingAccessService) {
        this.examAppealQueryRepository = examAppealQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.examAppealAccessService = examAppealAccessService;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppealSummaryInfo> execute(SearchClassTestAppealsQuery input) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        examGradingAccessService.authorizeClassTestChair(input.examId(), currentUserId);
        var schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));
        return examAppealQueryRepository.searchAppeals(
            schoolId, input.examId(), input.status(), input.keyword(), input.page(), input.size());
    }
}
