package com.sep.vox.application.port.input.usecase.assessmentgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewClassTestGradingResultsQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.GradingAssignmentFilter;
import com.sep.vox.application.projection.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.shared.PageResult;

/**
 * MỌI bài của một bài kiểm tra trên lớp, gồm cả bài CHƯA có phân công.
 *
 * <p>Khác {@link ViewMyClassTestGradingTasksUseCase}, vốn chỉ trả những bài đã giao cho
 * người gọi: bài chấm AI sạch đi thẳng sang {@code RELEASED} nên không được mở phân công
 * tự động, và trước khi có màn này thì giáo viên không có đường nào nhìn thấy — cụ thể là
 * lượt thi thứ hai của một em biến mất khỏi màn chấm. Nhận chấm cần {@code candidateResultId},
 * mà chỗ duy nhất phát ra id đó cho giáo viên chính là đây.
 *
 * <p>Dùng lại {@code searchAssignments} của bảng điều phối thay vì viết query mới: cùng
 * một câu hỏi ("mọi bài trong phạm vi, kèm phân công nếu có"), chỉ khác phạm vi và ai
 * được hỏi. Phạm vi ở đây đóng đúng bằng bài mà người gọi làm CHAIR.
 */
@Service
public class ViewClassTestGradingResultsUseCase
        implements IUseCase<ViewClassTestGradingResultsQuery, PageResult<GradingAssignmentRowInfo>> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public ViewClassTestGradingResultsUseCase(
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingAssignmentRowInfo> execute(ViewClassTestGradingResultsQuery input) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        assessmentGradingAccessService.authorizeClassTestChair(input.assessmentId(), currentUserId);
        var schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);

        return assessmentGradingQueryRepository.searchAssignments(new GradingAssignmentFilter(
            schoolId,
            input.assessmentId(),
            null,
            null,
            input.resultStatus(),
            null,
            null,
            input.unassignedOnly(),
            false,
            null,
            input.search(),
            // Khoá loại bài dù assessmentId đã thu hẹp phạm vi: quyền ở trên chỉ chấp nhận bài
            // trên lớp, nên đây là hàng rào thứ hai cho cùng một luật.
            AssessmentKind.CLASS_TEST.name()
        ), input.page(), input.size());
    }
}
