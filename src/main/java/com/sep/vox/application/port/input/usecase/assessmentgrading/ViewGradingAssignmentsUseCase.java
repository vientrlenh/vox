package com.sep.vox.application.port.input.usecase.assessmentgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.SearchGradingAssignmentsQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.GradingAssignmentFilter;
import com.sep.vox.application.projection.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.service.assessment.GradingScopeKind;
import com.sep.vox.domain.shared.PageResult;

/**
 * Bảng phân công của school admin. Phạm vi luôn bị khoá vào trường của người gọi, và
 * mặc định vào kỳ thi TẬP TRUNG.
 *
 * <p>Mặc định chứ không khoá cứng: cùng bảng này còn phục vụ màn nhà trường THEO DÕI
 * tiến độ chấm của một bài kiểm tra trên lớp (chỉ đọc, có sẵn assessmentId). Khoá cứng ở đây
 * là làm vỡ màn đó; để trống thì hai loại bài lại trộn chung như lỗi đang sửa.
 */
@Service
public class ViewGradingAssignmentsUseCase
        implements IUseCase<SearchGradingAssignmentsQuery, PageResult<GradingAssignmentRowInfo>> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public ViewGradingAssignmentsUseCase(
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingAssignmentRowInfo> execute(SearchGradingAssignmentsQuery input) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        var schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        return assessmentGradingQueryRepository.searchAssignments(new GradingAssignmentFilter(
            schoolId,
            input.assessmentId(),
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
        ), input.pageRequest().repositoryPage(), input.pageRequest().size());
    }
}
