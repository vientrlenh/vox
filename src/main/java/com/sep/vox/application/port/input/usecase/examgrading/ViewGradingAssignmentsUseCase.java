package com.sep.vox.application.port.input.usecase.examgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.SearchGradingAssignmentsQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.service.exam.GradingScopeKind;

/**
 * Bảng phân công của school admin. Phạm vi luôn bị khoá vào trường của người gọi, và
 * mặc định vào kỳ thi TẬP TRUNG.
 *
 * <p>Mặc định chứ không khoá cứng: cùng bảng này còn phục vụ màn nhà trường THEO DÕI
 * tiến độ chấm của một bài kiểm tra trên lớp (chỉ đọc, có sẵn examId). Khoá cứng ở đây
 * là làm vỡ màn đó; để trống thì hai loại bài lại trộn chung như lỗi đang sửa.
 */
@Service
public class ViewGradingAssignmentsUseCase
        implements IUseCase<SearchGradingAssignmentsQuery, PageResult<GradingAssignmentRowInfo>> {

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewGradingAssignmentsUseCase(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingAssignmentRowInfo> execute(SearchGradingAssignmentsQuery input) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var schoolId = examGradingAccessService.requireCurrentSchoolId(currentUserId);
        return examGradingQueryRepository.searchAssignments(new GradingAssignmentFilter(
            schoolId,
            input.examId(),
            input.scheduleId(),
            input.teacherId(),
            input.resultStatus(),
            input.roundType(),
            input.status(),
            input.unassignedOnly(),
            input.overdueOnly(),
            input.hasOpenAppeal(),
            input.search(),
            GradingScopeKind.orCentralized(input.kind())
        ), input.page(), input.size());
    }
}
