package com.sep.vox.application.port.input.usecase.examappeal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.SearchExamAppealsQuery;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.repository.ExamAppealQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewExamAppealsUseCase implements IUseCase<SearchExamAppealsQuery, PageResult<AppealSummaryInfo>> {

    private final ExamAppealQueryRepository examAppealQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final ExamAppealAccessService examAppealAccessService;

    public ViewExamAppealsUseCase(
            ExamAppealQueryRepository examAppealQueryRepository,
            SchoolUserRepository schoolUserRepository,
            ExamAppealAccessService examAppealAccessService) {
        this.examAppealQueryRepository = examAppealQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.examAppealAccessService = examAppealAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppealSummaryInfo> execute(SearchExamAppealsQuery input) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var schoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));
        return examAppealQueryRepository.searchAppeals(
            schoolId, input.status(), input.keyword(), input.page(), input.size());
    }
}
