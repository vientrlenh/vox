package com.sep.vox.application.port.input.usecase.examappeal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.application.query.repository.ExamAppealQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewExamAppealStatsUseCase {

    private final ExamAppealQueryRepository examAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final ExamAppealAccessService examAppealAccessService;

    public ViewExamAppealStatsUseCase(
            ExamAppealQueryRepository examAppealQueryRepository,
            SchoolUserRepository schoolUserRepository,
            ExamAppealAccessService examAppealAccessService) {
        this.examAppealQueryRepository = examAppealQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.examAppealAccessService = examAppealAccessService;
    }

    @Transactional(readOnly = true)
    public AppealStatsInfo execute() {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));
        return examAppealQueryRepository.countByStatus(schoolId);
    }
}
