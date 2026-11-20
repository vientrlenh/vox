package com.sep.vox.application.port.input.usecase.examgrading;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAssignableTeachersQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AssignableTeacherInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;

/** Nguồn chung cho cả modal auto-assign lẫn dropdown gán tay / đổi giáo viên. */
@Service
public class ViewAssignableTeachersUseCase
        implements IUseCase<ViewAssignableTeachersQuery, List<AssignableTeacherInfo>> {

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewAssignableTeachersUseCase(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignableTeacherInfo> execute(ViewAssignableTeachersQuery input) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var schoolId = examGradingAccessService.requireCurrentSchoolId(currentUserId);
        return examGradingQueryRepository.findAssignableTeachers(schoolId, input.search());
    }
}
