package com.sep.vox.application.port.input.usecase.examgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewClassTestGradingResultsQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamKind;

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

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewClassTestGradingResultsUseCase(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingAssignmentRowInfo> execute(ViewClassTestGradingResultsQuery input) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        examGradingAccessService.authorizeClassTestChair(input.examId(), currentUserId);
        var schoolId = examGradingAccessService.requireCurrentSchoolId(currentUserId);

        return examGradingQueryRepository.searchAssignments(new GradingAssignmentFilter(
            schoolId,
            input.examId(),
            null,
            null,
            input.resultStatus(),
            null,
            null,
            input.unassignedOnly(),
            false,
            null,
            input.search(),
            // Khoá loại bài dù examId đã thu hẹp phạm vi: quyền ở trên chỉ chấp nhận bài
            // trên lớp, nên đây là hàng rào thứ hai cho cùng một luật.
            ExamKind.CLASS_TEST.name()
        ), input.page(), input.size());
    }
}
