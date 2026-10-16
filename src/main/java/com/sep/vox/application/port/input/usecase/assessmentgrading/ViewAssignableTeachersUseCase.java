package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAssignableTeachersQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.AssignableTeacherInfo;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;

/** Nguồn chung cho cả modal auto-assign lẫn dropdown gán tay / đổi giáo viên. */
@Service
public class ViewAssignableTeachersUseCase
        implements IUseCase<ViewAssignableTeachersQuery, List<AssignableTeacherInfo>> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public ViewAssignableTeachersUseCase(
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignableTeacherInfo> execute(ViewAssignableTeachersQuery input) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        var schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        return assessmentGradingQueryRepository.findAssignableTeachers(schoolId, input.search());
    }
}
