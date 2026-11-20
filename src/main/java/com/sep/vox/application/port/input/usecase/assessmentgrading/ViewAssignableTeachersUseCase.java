package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.List;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAssignableTeachersQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AssignableTeacherInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;

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
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        UUID schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        return assessmentGradingQueryRepository.findAssignableTeachers(schoolId, input.search());
    }
}
