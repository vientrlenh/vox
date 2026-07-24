package com.sep.vox.application.port.input.usecase.examgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.SearchGradingAssignmentsQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;

/** Bảng phân công của school admin. Phạm vi luôn bị khoá vào trường của người gọi. */
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
        return examGradingQueryRepository.searchAssignments(
            schoolId, input.examId(), input.scheduleId(), input.teacherId(),
            input.status(), input.search(), input.page(), input.size());
    }
}
